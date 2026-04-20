package local.ims.task1.resource_service.repositories;

import local.ims.task1.resource_service.entities.Mp3Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mp3ResourceRepository extends JpaRepository<Mp3Resource, Integer> {
}

