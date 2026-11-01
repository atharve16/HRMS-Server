package com.hrms.employee.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.entity.User;
import com.hrms.auth.entity.UserRole;
import com.hrms.auth.entity.UserStatus;
import com.hrms.auth.repository.UserRepository;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.ApiException;
import com.hrms.common.exceptions.ConflictException;
import com.hrms.common.exceptions.ErrorCode;
import com.hrms.common.exceptions.ResourceNotFoundException;
import com.hrms.employee.dto.CreateEmployeeRequest;
import com.hrms.employee.dto.EmployeePageResponse;
import com.hrms.employee.dto.EmployeeResponse;
import com.hrms.employee.dto.UpdateEmployeeRequest;
import com.hrms.employee.mapper.EmployeeMapper;
import com.hrms.employee.model.Employee;
import com.hrms.employee.model.EmploymentStatus;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.EmployeeSearch;
import com.hrms.department.service.DepartmentService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EmployeeCodeGenerator codeGenerator;
    private final AuditService auditService;
    private final com.hrms.auth.service.EmailNormalizer emailNormalizer;
    private final Clock clock;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,
                           EmployeeCodeGenerator codeGenerator, AuditService auditService,
                           com.hrms.auth.service.EmailNormalizer emailNormalizer, Clock clock) {
        this(employeeRepository, userRepository, codeGenerator, auditService, emailNormalizer, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,
                           EmployeeCodeGenerator codeGenerator, AuditService auditService,
                           com.hrms.auth.service.EmailNormalizer emailNormalizer, Clock clock,
                           DepartmentService departmentService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.codeGenerator = codeGenerator;
        this.auditService = auditService;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
        this.departmentService = departmentService;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request, AuthPrincipal actor) {
        String email = emailNormalizer.normalize(request.email());
        validateDepartmentAndManager(request.departmentId(), request.reportingManagerId(), null);
        if (userRepository.findByNormalizedEmail(email).isPresent() || employeeRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("An account already exists for this email address");
        }
        String employeeId = new ObjectId().toHexString();
        String userId = new ObjectId().toHexString();
        Employee employee = EmployeeMapper.from(request);
        employee.setId(employeeId);
        employee.setUserId(userId);
        employee.setEmployeeCode(codeGenerator.next());
        employee.setEmail(email);
        employee.setCreatedBy(actor.userId());
        employee.setUpdatedBy(actor.userId());
        User user = User.employee(userId, email, employeeId);
        employeeRepository.save(employee);
        userRepository.save(user);
        auditService.record(actor, "EMPLOYEE_CREATED", "EMPLOYEE", employeeId, Map.of(),
                Map.of("employeeCode", employee.getEmployeeCode(), "userId", userId, "email", email), null);
        return EmployeeResponse.from(employee);
    }

    public EmployeePageResponse list(EmployeeSearch search) {
        if (search.page() < 0 || search.limit() < 1 || search.limit() > 100) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and limit must be between 1 and 100");
        }
        Page<Employee> page = employeeRepository.search(search);
        return EmployeePageResponse.from(page, search.limit());
    }

    public EmployeeResponse get(String employeeId) {
        return EmployeeResponse.from(find(employeeId));
    }

    @Transactional
    public EmployeeResponse update(String employeeId, UpdateEmployeeRequest request, AuthPrincipal actor) {
        Employee employee = find(employeeId);
        if (request.terminationDate() != null && request.terminationDate().isBefore(employee.getJoiningDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Termination date cannot be earlier than joining date");
        }
        validateDepartmentAndManager(request.departmentId(), request.reportingManagerId(), employeeId);
        User user = linkedUser(employee);
        Map<String, Object> before = snapshot(employee);
        List<String> changed = changedFields(request);
        String oldEmail = employee.getEmail();
        if (request.email() != null) {
            String newEmail = emailNormalizer.normalize(request.email());
            if (!newEmail.equals(oldEmail)) {
                ensureEmailAvailable(newEmail, employee.getId(), user.getId());
                employee.setEmail(newEmail);
                user.setEmail(newEmail);
                user.setNormalizedEmail(newEmail);
                changed.add("email");
            }
        }
        EmployeeMapper.apply(request, employee);
        employee.setUpdatedBy(actor.userId());
        employeeRepository.save(employee);
        if (request.email() != null) {
            user.setEmail(employee.getEmail());
            user.setNormalizedEmail(employee.getEmail());
        }
        userRepository.save(user);
        auditService.record(actor, "EMPLOYEE_UPDATED", "EMPLOYEE", employee.getId(), before,
                Map.of("fields", changed), null);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse deactivate(String employeeId, AuthPrincipal actor) {
        Employee employee = find(employeeId);
        if (employee.getEmploymentStatus() == EmploymentStatus.INACTIVE) return EmployeeResponse.from(employee);
        if (employee.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new ConflictException("A terminated employee cannot be deactivated again");
        }
        User user = linkedUser(employee);
        employee.setEmploymentStatus(EmploymentStatus.INACTIVE);
        employee.setUpdatedBy(actor.userId());
        user.setStatus(UserStatus.INACTIVE);
        employeeRepository.save(employee);
        userRepository.save(user);
        auditService.record(actor, "EMPLOYEE_DEACTIVATED", "EMPLOYEE", employee.getId(),
                Map.of("employmentStatus", EmploymentStatus.ACTIVE.name()),
                Map.of("employmentStatus", EmploymentStatus.INACTIVE.name()), null);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse reactivate(String employeeId, AuthPrincipal actor) {
        Employee employee = find(employeeId);
        if (employee.getEmploymentStatus() == EmploymentStatus.ACTIVE) return EmployeeResponse.from(employee);
        if (employee.getEmploymentStatus() == EmploymentStatus.TERMINATED) {
            throw new ConflictException("A terminated employee cannot be reactivated");
        }
        User user = linkedUser(employee);
        employee.setEmploymentStatus(EmploymentStatus.ACTIVE);
        employee.setUpdatedBy(actor.userId());
        user.setStatus(UserStatus.ACTIVE);
        employeeRepository.save(employee);
        userRepository.save(user);
        auditService.record(actor, "EMPLOYEE_REACTIVATED", "EMPLOYEE", employee.getId(),
                Map.of("employmentStatus", EmploymentStatus.INACTIVE.name()),
                Map.of("employmentStatus", EmploymentStatus.ACTIVE.name()), null);
        return EmployeeResponse.from(employee);
    }

    public EmployeeResponse current(AuthPrincipal principal) {
        Employee employee = employeeRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile was not found"));
        if (!principal.userId().equals(employee.getUserId())) {
            throw new ApiException(ErrorCode.EMPLOYEE_ACCOUNT_INCONSISTENT,
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "The employee account link is inconsistent");
        }
        return EmployeeResponse.from(employee);
    }

    private Employee find(String employeeId) {
        if (!ObjectId.isValid(employeeId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Employee id is invalid");
        }
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee was not found"));
    }

    private User linkedUser(Employee employee) {
        User user = userRepository.findById(employee.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.EMPLOYEE_ACCOUNT_INCONSISTENT,
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "The employee account link is inconsistent"));
        if (user.getRole() != UserRole.EMPLOYEE || !employee.getId().equals(user.getEmployeeId())
                || !employee.getEmail().equals(user.getNormalizedEmail())
                || !employee.getEmail().equals(user.getEmail())) {
            throw new ApiException(ErrorCode.EMPLOYEE_ACCOUNT_INCONSISTENT,
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "The employee account link is inconsistent");
        }
        return user;
    }

    private void ensureEmailAvailable(String email, String employeeId, String userId) {
        userRepository.findByNormalizedEmail(email).ifPresent(user -> {
            if (!user.getId().equals(userId)) throw new ConflictException("An account already exists for this email address");
        });
        employeeRepository.findByEmail(email).ifPresent(other -> {
            if (!other.getId().equals(employeeId)) throw new ConflictException("An employee already exists for this email address");
        });
    }

    private Map<String, Object> snapshot(Employee employee) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("firstName", employee.getFirstName());
        snapshot.put("lastName", employee.getLastName());
        snapshot.put("email", employee.getEmail());
        snapshot.put("employmentStatus", employee.getEmploymentStatus().name());
        snapshot.put("designation", employee.getDesignation());
        return snapshot;
    }

    private List<String> changedFields(UpdateEmployeeRequest request) {
        List<String> fields = new ArrayList<>();
        if (request.firstName() != null) fields.add("firstName");
        if (request.lastName() != null) fields.add("lastName");
        if (request.phone() != null) fields.add("phone");
        if (request.dateOfBirth() != null) fields.add("dateOfBirth");
        if (request.gender() != null) fields.add("gender");
        if (request.profileImageUrl() != null) fields.add("profileImageUrl");
        if (request.address() != null) fields.add("address");
        if (request.designation() != null) fields.add("designation");
        if (request.departmentId() != null) fields.add("departmentId");
        if (request.employmentType() != null) fields.add("employmentType");
        if (request.reportingManagerId() != null) fields.add("reportingManagerId");
        if (request.workLocation() != null) fields.add("workLocation");
        if (request.emergencyContact() != null) fields.add("emergencyContact");
        if (request.terminationDate() != null) fields.add("terminationDate");
        return fields;
    }

    private void validateDepartmentAndManager(String departmentId, String managerId, String employeeId) {
        if (departmentService == null) return;
        departmentService.validateActive(departmentId);
        if (managerId != null && managerId.equals(employeeId)) {
            throw new ConflictException("An employee cannot report to themselves");
        }
        if (managerId != null) {
            if (!ObjectId.isValid(managerId)) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Reporting manager id is invalid");
            }
            employeeRepository.findById(managerId)
                    .filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                    .orElseThrow(() -> new ConflictException("Reporting manager must be an active employee"));
        }
    }
}
