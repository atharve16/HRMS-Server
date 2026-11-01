package com.hrms.shift.repository;
import com.hrms.shift.model.Shift;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ShiftRepository extends MongoRepository<Shift,String>, ShiftRepositoryCustom { boolean existsByShiftCode(String code); }
