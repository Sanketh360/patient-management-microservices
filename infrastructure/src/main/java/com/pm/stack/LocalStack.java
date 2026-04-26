package com.pm.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.*;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;
    private final boolean isLocal;

    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope, id, props);


        this.isLocal = System.getenv("LOCALSTACK") != null;

        this.vpc = createVpc();
        this.ecsCluster = createEcsCluster();

        DatabaseInstance authServiceDb = null;
        DatabaseInstance patientServiceDb = null;
        CfnCluster mskCluster = null;

        // ✅ Only create real AWS infra if NOT local
        if (!isLocal) {
            authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");
            patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

            createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
            createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

            mskCluster = createMskCluster();
        }

        // 🔹 Services
        FargateService authService = createFargateService(
                "AuthService",
                "auth-service",
                List.of(4005),
                authServiceDb,
                Map.of("JWT_SECRET", "a1e4113c7e878479e2c98112c8625684af385fc12a343cc70e33934ee03ae199")
        );

        FargateService patientService = createFargateService(
                "PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                )
        );

        if (!isLocal && mskCluster != null) {
            patientService.getNode().addDependency(mskCluster);
        }

        createApiGatewayService();
    }

    private Vpc createVpc(){
        return Vpc.Builder.create(this, "VPC")
                .maxAzs(2)
                .build();
    }

    private DatabaseInstance createDatabase(String id, String dbName){
        return DatabaseInstance.Builder.create(this, id)
                .engine(DatabaseInstanceEngine.postgres(
                        PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .credentials(Credentials.fromGeneratedSecret("admin"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder.create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP")
                        .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                        .ipAddress(db.getDbInstanceEndpointAddress())
                        .requestInterval(30)
                        .failureThreshold(3)
                        .build())
                .build();
    }

    private CfnCluster createMskCluster(){
        return CfnCluster.Builder.create(this, "Kafka")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.large")
                        .clientSubnets(vpc.getPrivateSubnets().stream()
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                        .build())
                .build();
    }

    private Cluster createEcsCluster(){
        return Cluster.Builder.create(this, "Cluster")
                .vpc(vpc)
                .build();
    }

    private FargateService createFargateService(
            String id,
            String imageName,
            List<Integer> ports,
            DatabaseInstance db,
            Map<String, String> additionalEnvVars
    ) {

        FargateTaskDefinition task = FargateTaskDefinition.Builder.create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        Map<String, String> env = new HashMap<>();

        // 🔥 LOCAL vs CLOUD DB config
        if (isLocal) {
            env.put("SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://host.docker.internal:5432/" + imageName);
            env.put("SPRING_DATASOURCE_USERNAME", "postgres");
            env.put("SPRING_DATASOURCE_PASSWORD", "postgres");
        } else if (db != null) {
            env.put("SPRING_DATASOURCE_URL",
                    "jdbc:postgresql://" +
                            db.getDbInstanceEndpointAddress() + ":" +
                            db.getDbInstanceEndpointPort());
        }

        if (additionalEnvVars != null) {
            env.putAll(additionalEnvVars);
        }

        task.addContainer(id + "Container",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .environment(env)
                        .portMappings(
                                ports.stream().map(p ->
                                        PortMapping.builder()
                                                .containerPort(p)
                                                .hostPort(p)
                                                .build()
                                ).toList()
                        )
                        .logging(LogDriver.awsLogs(
                                AwsLogDriverProps.builder()
                                        .streamPrefix(imageName)
                                        .logGroup(LogGroup.Builder.create(this, id + "Logs")
                                                .retention(RetentionDays.ONE_DAY)
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .build()
                        ))
                        .build()
        );

        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(task)
                .build();
    }

    private void createApiGatewayService() {
        FargateTaskDefinition task = FargateTaskDefinition.Builder.create(this, "GatewayTask")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        task.addContainer("Gateway",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry("api-gateway"))
                        .environment(Map.of(
                                "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                        ))
                        .portMappings(List.of(
                                PortMapping.builder().containerPort(4004).build()
                        ))
                        .build()
        );

        ApplicationLoadBalancedFargateService.Builder.create(this, "GatewayService")
                .cluster(ecsCluster)
                .taskDefinition(task)
                .build();
    }

    public static void main(final String[] args) {
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        new LocalStack(app, "localstack",
                StackProps.builder()
                        .synthesizer(new BootstraplessSynthesizer())
                        .build());

        app.synth();
    }


}
