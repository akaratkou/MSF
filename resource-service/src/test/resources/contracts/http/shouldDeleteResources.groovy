package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should delete resources and return deleted IDs"
    
    request {
        method DELETE()
        url("/resources") {
            queryParameters {
                parameter("id", "1,2,3")
            }
        }
    }
    
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            ids: [1, 2, 3]
        ])
    }
}
