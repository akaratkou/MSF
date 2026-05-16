package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should publish resource uploaded event to RabbitMQ"
    label "resource_uploaded"
    
    input {
        triggeredBy("publishResourceUploadedEvent()")
    }
    
    outputMessage {
        sentTo("resource.uploaded")
        body([
            resourceId: $(consumer(anyNumber()), producer(123))
        ])
        headers {
            messagingContentType(applicationJson())
        }
    }
}
