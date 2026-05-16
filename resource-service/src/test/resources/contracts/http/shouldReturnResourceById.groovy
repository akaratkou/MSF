package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return resource by ID"

    request {
        method GET()
        url "/resources/123"
    }

    response {
        status 200
        headers {
            contentType("audio/mpeg")
        }
        body(anyNonEmptyString())
    }
}
