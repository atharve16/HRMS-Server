package com.hrms.payroll.repository;
import com.hrms.payroll.model.SalaryStructure;
import org.springframework.data.mongodb.repository.*;
import java.time.LocalDate;
import java.util.*;

public interface SalaryStructureRepository extends MongoRepository<SalaryStructure,String> {
    Optional<SalaryStructure> findByEmployeeIdAndActiveTrue(String employeeId);
    List<SalaryStructure> findByEmployeeIdOrderByEffectiveFromDesc(String employeeId);
    @Query("{'employeeId':?0,'effectiveFrom':{$lte:?1},$or:[{'effectiveTo':null},{'effectiveTo':{$gte:?1}}]}")
    List<SalaryStructure> findApplicable(String employeeId, LocalDate date);
}
