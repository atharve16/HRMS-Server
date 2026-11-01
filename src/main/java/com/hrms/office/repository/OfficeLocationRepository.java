package com.hrms.office.repository;
import com.hrms.office.model.OfficeLocation;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface OfficeLocationRepository extends MongoRepository<OfficeLocation,String>, OfficeLocationRepositoryCustom {
    boolean existsByLocationCode(String code);
    Optional<OfficeLocation> findByLocationCode(String code);
}
