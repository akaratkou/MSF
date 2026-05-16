package local.ims.task1.resource_service.component;

import io.cucumber.spring.CucumberContextConfiguration;
import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import local.ims.task1.resource_service.services.ResourceMessageSender;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class CucumberSpringConfiguration {

    // Must run first — LocalStackContainer constructor already calls Docker
    static {
        System.setProperty("DOCKER_HOST", "tcp://127.0.0.1:2375");
        System.setProperty("DOCKER_TLS_VERIFY", "0");
    }

    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17").withInitScript("init.sql");

    static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
                    .withServices(LocalStackContainer.Service.S3);

    static final RabbitMQContainer rabbitmq =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management-alpine"));

    static {
        postgres.start();
        localstack.start();
        rabbitmq.start();
        try {
            localstack.execInContainer("awslocal", "s3", "mb", "s3://resource-bucket");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create S3 bucket in LocalStack", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",         postgres::getJdbcUrl);
        r.add("spring.datasource.username",    postgres::getUsername);
        r.add("spring.datasource.password",    postgres::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        r.add("spring.cloud.aws.s3.endpoint",
                () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        r.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        r.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        r.add("spring.cloud.aws.region.static",          localstack::getRegion);

        r.add("spring.rabbitmq.host",     rabbitmq::getHost);
        r.add("spring.rabbitmq.port",     () -> rabbitmq.getAmqpPort().toString());
        r.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        r.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        r.add("eureka.client.enabled",          () -> "false");
        r.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @MockitoBean ResourceMessageSender resourceMessageSender;
    @MockitoBean MetadataServiceClient  metadataServiceClient;
}
