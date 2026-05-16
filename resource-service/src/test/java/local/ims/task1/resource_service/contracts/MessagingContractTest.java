package local.ims.task1.resource_service.contracts;

import local.ims.task1.resource_service.interfaces.MetadataServiceClient;
import local.ims.task1.resource_service.services.ResourceMessageSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messaging Contract Test (manual) — verifies the "resource.uploaded" contract:
 *   When a resource is uploaded, a message {"resourceId": <id>} is published
 *   to the "resource.uploaded" queue via "resource.exchange".
 *
 * This is the producer-side proof that the messaging contract is honoured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("Messaging Contract: resource uploaded event")
class MessagingContractTest {

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
            throw new RuntimeException("Failed to create S3 bucket", e);
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

    @MockitoBean
    MetadataServiceClient metadataServiceClient;

    @Autowired
    ResourceMessageSender resourceMessageSender;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Contract: sendResourceUploaded publishes {resourceId} to resource.uploaded queue")
    void shouldPublishResourceUploadedEvent() {
        // given
        Integer resourceId = 123;

        // when — trigger the contract (same as groovy contract's triggeredBy)
        resourceMessageSender.sendResourceUploaded(resourceId);

        // then — receive message from the queue (contract: sentTo "resource.uploaded")
        @SuppressWarnings("unchecked")
        Map<String, Object> received = (Map<String, Object>) rabbitTemplate
                .receiveAndConvert("resource.uploaded", 5_000);

        assertThat(received).isNotNull();
        // contract body: resourceId is a number equal to the sent ID
        assertThat(received).containsKey("resourceId");
        assertThat(((Number) received.get("resourceId")).intValue()).isEqualTo(resourceId);
    }
}

