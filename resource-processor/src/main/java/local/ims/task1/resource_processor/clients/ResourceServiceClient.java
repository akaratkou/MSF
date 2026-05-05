package local.ims.task1.resource_processor.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "${services.resource-service.name:resource-service}")
public interface ResourceServiceClient {

    @GetMapping(value = "/resources/{id}", consumes = "audio/mpeg")
    byte[] getResourceById(@PathVariable("id") Integer id);
}
