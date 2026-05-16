package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return 404 for non-existent resource"
    
    request {
        method GET()
        url "/resources/9999"
    }
    
    response {
        status 404
        headers {
            contentType(applicationJson())
        }
        body([
            errorMessage: "Resource with ID=9999 not found"
        ])
    }
}
