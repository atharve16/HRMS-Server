package com.hrms.payroll.repository;
import com.hrms.payroll.model.PayrollRecord;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class PayrollRecordRepositoryImpl implements PayrollRecordRepositoryCustom {
    private final MongoTemplate mongo;
    public PayrollRecordRepositoryImpl(MongoTemplate mongo){this.mongo=mongo;}
    public Page<PayrollRecord> search(PayrollSearch s){
        List<Criteria> filters=new ArrayList<>();
        if(s.month()!=null) filters.add(Criteria.where("payrollMonth").is(s.month()));
        if(s.year()!=null) filters.add(Criteria.where("payrollYear").is(s.year()));
        if(s.employeeId()!=null&&!s.employeeId().isBlank()) filters.add(Criteria.where("employeeId").is(s.employeeId()));
        if(s.employeeCode()!=null&&!s.employeeCode().isBlank()) filters.add(Criteria.where("employeeCodeSnapshot").is(s.employeeCode().strip()));
        if(s.departmentId()!=null&&!s.departmentId().isBlank()) filters.add(Criteria.where("departmentIdSnapshot").is(s.departmentId()));
        if(s.status()!=null) filters.add(Criteria.where("status").is(s.status()));
        Criteria criteria=filters.isEmpty()?new Criteria():new Criteria().andOperator(filters);
        Sort.Direction direction="asc".equalsIgnoreCase(s.sortDirection())?Sort.Direction.ASC:Sort.Direction.DESC;
        Pageable pageable=PageRequest.of(s.page(),s.limit(),Sort.by(direction,"createdAt"));
        Query query=Query.query(criteria).with(pageable);
        return new PageImpl<>(mongo.find(query,PayrollRecord.class),pageable,mongo.count(Query.query(criteria),PayrollRecord.class));
    }
}
