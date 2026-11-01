package com.hrms.payroll.service;

import com.hrms.audit.service.AuditService;
import com.hrms.auth.security.AuthPrincipal;
import com.hrms.common.exceptions.*;
import com.hrms.common.utils.CompanyTimeProvider;
import com.hrms.employee.model.*;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.*;
import com.hrms.payroll.model.*;
import com.hrms.payroll.repository.*;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
public class PayrollService {
    private static final Set<PayrollStatus> EMPLOYEE_VISIBLE=EnumSet.of(PayrollStatus.FINALIZED,PayrollStatus.PAID);
    private final PayrollRecordRepository payrolls;private final EmployeeRepository employees;private final SalaryStructureService salaries;
    private final PayrollCalculationService calculator;private final AuditService audit;private final CompanyTimeProvider time;private final TransactionTemplate transactions;
    public PayrollService(PayrollRecordRepository payrolls,EmployeeRepository employees,SalaryStructureService salaries,
            PayrollCalculationService calculator,AuditService audit,CompanyTimeProvider time,TransactionTemplate transactions){
        this.payrolls=payrolls;this.employees=employees;this.salaries=salaries;this.calculator=calculator;this.audit=audit;this.time=time;this.transactions=transactions;}

    public PayrollGenerationSummary generate(GeneratePayrollRequest request,AuthPrincipal actor){
        YearMonth month=YearMonth.of(request.year(),request.month());List<PayrollGenerationItem> generated=new ArrayList<>(),skipped=new ArrayList<>(),failed=new ArrayList<>();
        for(Employee employee:employees.findAll()){
            if(!eligible(employee,month))continue;
            if(payrolls.existsByEmployeeIdAndPayrollMonthAndPayrollYear(employee.getId(),request.month(),request.year())){
                skipped.add(item(employee,null,"Payroll already exists"));continue;}
            try{
                LocalDate salaryDate=employee.getTerminationDate()==null?month.atEndOfMonth():min(month.atEndOfMonth(),employee.getTerminationDate());
                Optional<SalaryStructure> salary=salaries.applicable(employee.getId(),salaryDate);
                if(salary.isEmpty()){skipped.add(item(employee,null,"No salary structure applies to the payroll period"));continue;}
                PayrollRecord calculated=calculator.calculate(employee,salary.get(),request.month(),request.year(),actor.userId());
                PayrollRecord saved=transactions.execute(status->{
                    if(payrolls.existsByEmployeeIdAndPayrollMonthAndPayrollYear(employee.getId(),request.month(),request.year()))
                        throw new ApiException(ErrorCode.PAYROLL_ALREADY_EXISTS,HttpStatus.CONFLICT,"Payroll already exists");
                    PayrollRecord record=payrolls.save(calculated);
                    audit.record(actor,"PAYROLL_GENERATED","PAYROLL_RECORD",record.getId(),Map.of(),Map.of("employeeId",employee.getId(),
                            "month",request.month(),"year",request.year(),"netSalary",record.getNetSalary()),null);return record;});
                generated.add(item(employee,saved.getId(),null));
            }catch(DuplicateKeyException|org.springframework.dao.OptimisticLockingFailureException ex){skipped.add(item(employee,null,"Payroll already exists or was generated concurrently"));
            }catch(ApiException ex){failed.add(item(employee,null,ex.getMessage()));
            }catch(RuntimeException ex){failed.add(item(employee,null,"Payroll generation failed: "+ex.getMessage()));}
        }
        return new PayrollGenerationSummary(request.month(),request.year(),generated,skipped,failed);
    }
    public PayrollPageResponse list(PayrollSearch search){validatePage(search.page(),search.limit());validateMonthYear(search.month(),search.year());return PayrollPageResponse.from(payrolls.search(search),search.limit());}
    public PayrollResponse detail(String id){return PayrollResponse.from(find(id));}

    @Transactional
    public PayrollResponse updateDraft(String id,UpdateDraftPayrollRequest input,AuthPrincipal actor){
        PayrollRecord p=find(id);require(p,PayrollStatus.DRAFT,"Only draft payroll can be adjusted");
        if(input.earnings()==null&&input.deductions()==null)throw validation("At least one adjustment list must be supplied");
        if(input.earnings()!=null)p.setEarnings(adjustments(input.earnings(),true,actor));
        if(input.deductions()!=null)p.setDeductions(adjustments(input.deductions(),false,actor));
        recalculate(p);p.setUpdatedBy(actor.userId());payrolls.save(p);
        audit.record(actor,"PAYROLL_ADJUSTMENTS_UPDATED","PAYROLL_RECORD",p.getId(),Map.of(),Map.of("employeeId",p.getEmployeeId(),
                "totalEarnings",p.getTotalEarnings(),"totalDeductions",p.getTotalDeductions(),"netSalary",p.getNetSalary()),null);
        return PayrollResponse.from(p);
    }
    @Transactional public PayrollResponse review(String id,AuthPrincipal actor){PayrollRecord p=find(id);require(p,PayrollStatus.DRAFT,"Only draft payroll can be reviewed");
        p.setStatus(PayrollStatus.REVIEWED);p.setReviewedBy(actor.userId());p.setReviewedAt(time.now());p.setUpdatedBy(actor.userId());payrolls.save(p);
        audit(actor,"PAYROLL_REVIEWED",p,"DRAFT","REVIEWED");return PayrollResponse.from(p);}
    @Transactional public PayrollResponse finalizePayroll(String id,AuthPrincipal actor){PayrollRecord p=find(id);require(p,PayrollStatus.REVIEWED,"Only reviewed payroll can be finalized");
        p.setStatus(PayrollStatus.FINALIZED);p.setFinalizedBy(actor.userId());p.setFinalizedAt(time.now());p.setUpdatedBy(actor.userId());payrolls.save(p);
        audit(actor,"PAYROLL_FINALIZED",p,"REVIEWED","FINALIZED");return PayrollResponse.from(p);}
    @Transactional public PayrollResponse pay(String id,AuthPrincipal actor){PayrollRecord p=find(id);require(p,PayrollStatus.FINALIZED,"Only finalized payroll can be marked paid");
        p.setStatus(PayrollStatus.PAID);p.setPaidBy(actor.userId());p.setPaidAt(time.now());p.setUpdatedBy(actor.userId());payrolls.save(p);
        audit(actor,"PAYROLL_PAID",p,"FINALIZED","PAID");return PayrollResponse.from(p);}

    public EmployeePayrollPageResponse mine(AuthPrincipal principal,Integer year,int page,int limit){validatePage(page,limit);Employee employee=employeeFor(principal);
        Pageable pageable=PageRequest.of(page,limit,Sort.by(Sort.Direction.DESC,"payrollYear","payrollMonth"));Page<PayrollRecord> result=year==null?
                payrolls.findByEmployeeIdAndStatusIn(employee.getId(),EMPLOYEE_VISIBLE,pageable):payrolls.findByEmployeeIdAndPayrollYearAndStatusIn(employee.getId(),year,EMPLOYEE_VISIBLE,pageable);
        return EmployeePayrollPageResponse.from(result,limit);}
    public EmployeePayrollResponse mineDetail(String id,AuthPrincipal principal){Employee employee=employeeFor(principal);validateId(id,"Payroll id is invalid");
        PayrollRecord p=payrolls.findByIdAndEmployeeIdAndStatusIn(id,employee.getId(),EMPLOYEE_VISIBLE).orElseThrow(()->new ResourceNotFoundException("Visible payroll record was not found"));return EmployeePayrollResponse.from(p);}

    private List<PayrollAdjustment> adjustments(List<PayrollAdjustmentInput> inputs,boolean earning,AuthPrincipal actor){List<PayrollAdjustment> result=new ArrayList<>();
        for(PayrollAdjustmentInput input:inputs){if(input.type().isEarning()!=earning)throw validation(earning?"Deduction types cannot be included in earnings":"Earning types cannot be included in deductions");
            PayrollAdjustment a=new PayrollAdjustment();a.setType(input.type());a.setDescription(input.description().strip());a.setAmount(money(input.amount()));a.setCreatedBy(actor.userId());a.setCreatedAt(time.now());result.add(a);}return result;}
    private void recalculate(PayrollRecord p){BigDecimal earnings=p.getEarnings().stream().map(PayrollAdjustment::getAmount).reduce(zero(),BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        BigDecimal otherDeductions=p.getDeductions().stream().map(PayrollAdjustment::getAmount).reduce(zero(),BigDecimal::add).setScale(2,RoundingMode.HALF_UP);
        BigDecimal totalDeductions=p.getSalaryDeduction().add(otherDeductions).setScale(2,RoundingMode.HALF_UP);
        BigDecimal net=p.getMonthlyFixedSalarySnapshot().subtract(p.getSalaryDeduction()).add(earnings).subtract(otherDeductions).setScale(2,RoundingMode.HALF_UP);
        if(net.signum()<0)throw validation("Adjustments would produce a negative net salary");p.setTotalEarnings(earnings);p.setTotalDeductions(totalDeductions);p.setNetSalary(net);}
    private void audit(AuthPrincipal actor,String action,PayrollRecord p,String before,String after){audit.record(actor,action,"PAYROLL_RECORD",p.getId(),Map.of("status",before),Map.of("employeeId",p.getEmployeeId(),"status",after),null);}
    private PayrollRecord find(String id){validateId(id,"Payroll id is invalid");return payrolls.findById(id).orElseThrow(()->new ResourceNotFoundException("Payroll record was not found"));}
    private Employee employeeFor(AuthPrincipal p){return employees.findByUserId(p.userId()).filter(e->e.getEmploymentStatus()==EmploymentStatus.ACTIVE)
            .orElseThrow(()->new ApiException(ErrorCode.EMPLOYEE_INACTIVE,HttpStatus.FORBIDDEN,"Employee account is inactive or unavailable"));}
    private static boolean eligible(Employee e,YearMonth month){if(e.getJoiningDate()==null||e.getJoiningDate().isAfter(month.atEndOfMonth()))return false;
        if(e.getTerminationDate()!=null&&e.getTerminationDate().isBefore(month.atDay(1)))return false;return e.getEmploymentStatus()==EmploymentStatus.ACTIVE||e.getTerminationDate()!=null;}
    private static void require(PayrollRecord p,PayrollStatus expected,String message){if(p.getStatus()!=expected)throw new ApiException(ErrorCode.PAYROLL_NOT_FINALIZABLE,HttpStatus.CONFLICT,message);}
    private static PayrollGenerationItem item(Employee e,String id,String reason){return new PayrollGenerationItem(e.getId(),e.getEmployeeCode(),id,reason);}
    private static BigDecimal money(BigDecimal v){if(v==null||v.signum()<=0)throw validation("Adjustment amounts must be greater than zero");return v.setScale(2,RoundingMode.HALF_UP);}
    private static BigDecimal zero(){return BigDecimal.ZERO.setScale(2);} private static LocalDate min(LocalDate a,LocalDate b){return a.isBefore(b)?a:b;}
    private static void validateId(String id,String message){if(!ObjectId.isValid(id))throw validation(message);}
    private static void validateMonthYear(Integer month,Integer year){if(month!=null&&(month<1||month>12))throw validation("Month must be between 1 and 12");if(year!=null&&(year<2000||year>2200))throw validation("Year is invalid");}
    private static void validatePage(int page,int limit){if(page<0||limit<1||limit>100)throw validation("Page must be non-negative and limit must be between 1 and 100");}
    private static ApiException validation(String message){return new ApiException(ErrorCode.VALIDATION_ERROR,HttpStatus.BAD_REQUEST,message);}
}
