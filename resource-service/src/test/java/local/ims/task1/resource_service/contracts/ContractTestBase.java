package local.ims.task1.resource_service.contracts;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import local.ims.task1.resource_service.exceptions.ResourceNotFoundException;
import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import local.ims.task1.resource_service.services.Mp3Service;
import local.ims.task1.resource_service.services.ResourceMessageSender;
import local.ims.task1.resource_service.services.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Base class for HTTP Spring Cloud Contract producer-side (verifier) tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class ContractTestBase {

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

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    Mp3Service mp3Service;

    @MockitoBean
    ResourceMessageSender resourceMessageSender;

    @MockitoBean
    MetadataServiceClient metadataServiceClient;

    @MockitoBean
    S3Service s3Service;

    @BeforeEach
    void setup() {
        byte[] fakeAudio = new byte[512];
        when(mp3Service.getMp3ById(123)).thenReturn(fakeAudio);
        when(mp3Service.getMp3ById(9999))
                .thenThrow(new ResourceNotFoundException("Resource with ID=9999 not found"));
        when(mp3Service.deleteResourcesWithCascading("1,2,3")).thenReturn(List.of(1, 2, 3));

        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
    }
}
