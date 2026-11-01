package com.hrms.department.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.department.dto.*;
import com.hrms.department.model.*;
import com.hrms.department.repository.*;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class DepartmentService {
    private final DepartmentRepository departments; private final EmployeeRepository employees; private final AuditService audit;
    public DepartmentService(DepartmentRepository departments, EmployeeRepository employees, AuditService audit){this.departments=departments;this.employees=employees;this.audit=audit;}
    @Transactional public DepartmentResponse create(CreateDepartmentRequest r, AuthPrincipal actor){
        String code=r.departmentCode().strip().toUpperCase(); String name=r.name().strip();
        if(departments.existsByDepartmentCode(code)||departments.findByNormalizedName(normalize(name)).isPresent())throw new ConflictException("Department code or name already exists");
        validateManager(r.managerEmployeeId(),null); Department d=new Department(); d.setId(new ObjectId().toHexString());d.setDepartmentCode(code);d.setName(name);d.setNormalizedName(normalize(name));d.setDescription(trim(r.description()));d.setManagerEmployeeId(trim(r.managerEmployeeId()));d.setStatus(DepartmentStatus.ACTIVE);d.setCreatedBy(actor.userId());d.setUpdatedBy(actor.userId());departments.save(d);audit.record(actor,"DEPARTMENT_CREATED","DEPARTMENT",d.getId(),Map.of(),Map.of("departmentCode",code,"name",name),null);return DepartmentResponse.from(d);
    }
    public DepartmentPageResponse list(DepartmentSearch s){checkPage(s.page(),s.limit());return DepartmentPageResponse.from(departments.search(s),s.limit());}
    public DepartmentResponse get(String id){return DepartmentResponse.from(find(id));}
    @Transactional public DepartmentResponse update(String id,UpdateDepartmentRequest r,AuthPrincipal actor){Department d=find(id);String oldName=d.getNormalizedName();String code=r.departmentCode()!=null?r.departmentCode().strip().toUpperCase():d.getDepartmentCode();String name=r.name()!=null?r.name().strip():d.getName();if(!code.equals(d.getDepartmentCode())&&departments.existsByDepartmentCode(code))throw new ConflictException("Department code already exists");if(!normalize(name).equals(oldName)&&departments.findByNormalizedName(normalize(name)).filter(x->!x.getId().equals(id)).isPresent())throw new ConflictException("Department name already exists");validateManager(r.managerEmployeeId(),null);d.setDepartmentCode(code);d.setName(name);d.setNormalizedName(normalize(name));if(r.description()!=null)d.setDescription(trim(r.description()));if(r.managerEmployeeId()!=null)d.setManagerEmployeeId(trim(r.managerEmployeeId()));d.setUpdatedBy(actor.userId());departments.save(d);audit.record(actor,"DEPARTMENT_UPDATED","DEPARTMENT",id,Map.of("name",oldName),Map.of("name",d.getName()),null);return DepartmentResponse.from(d);}
    @Transactional public DepartmentResponse deactivate(String id,AuthPrincipal actor){Department d=find(id);if(d.getStatus()==DepartmentStatus.INACTIVE)return DepartmentResponse.from(d);if(employees.existsByDepartmentIdAndEmploymentStatus(id,EmploymentStatus.ACTIVE))throw new ConflictException("Reassign active employees before deactivating this department");d.setStatus(DepartmentStatus.INACTIVE);d.setUpdatedBy(actor.userId());departments.save(d);audit.record(actor,"DEPARTMENT_DEACTIVATED","DEPARTMENT",id,Map.of("status","ACTIVE"),Map.of("status","INACTIVE"),null);return DepartmentResponse.from(d);}
    @Transactional public DepartmentResponse reactivate(String id,AuthPrincipal actor){Department d=find(id);if(d.getStatus()==DepartmentStatus.ACTIVE)return DepartmentResponse.from(d);d.setStatus(DepartmentStatus.ACTIVE);d.setUpdatedBy(actor.userId());departments.save(d);audit.record(actor,"DEPARTMENT_REACTIVATED","DEPARTMENT",id,Map.of("status","INACTIVE"),Map.of("status","ACTIVE"),null);return DepartmentResponse.from(d);}
    public void validateActive(String id){if(id==null||id.isBlank())return;Department d=find(id);if(d.getStatus()!=DepartmentStatus.ACTIVE)throw new ConflictException("Department is inactive");}
    private void validateManager(String managerId,String self){if(managerId==null||managerId.isBlank())return;if(managerId.equals(self))throw new ConflictException("An employee cannot manage themselves");if(!ObjectId.isValid(managerId))throw new ApiException(ErrorCode.VALIDATION_ERROR,org.springframework.http.HttpStatus.BAD_REQUEST,"Manager employee id is invalid");employees.findById(managerId).filter(e->e.getEmploymentStatus()==EmploymentStatus.ACTIVE).orElseThrow(()->new ConflictException("Department manager must be an active employee"));}
    private Department find(String id){if(!ObjectId.isValid(id))throw new ApiException(ErrorCode.VALIDATION_ERROR,org.springframework.http.HttpStatus.BAD_REQUEST,"Department id is invalid");return departments.findById(id).orElseThrow(()->new ResourceNotFoundException("Department was not found"));}
    private static String normalize(String s){return s.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+"," ");} private static String trim(String s){return s==null?null:s.strip();} private static void checkPage(int p,int l){if(p<0||l<1||l>100)throw new ApiException(ErrorCode.VALIDATION_ERROR,org.springframework.http.HttpStatus.BAD_REQUEST,"Page must be non-negative and limit must be between 1 and 100");}
}
