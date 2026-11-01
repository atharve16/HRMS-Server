package com.hrms.payroll.service;

import com.hrms.assignment.dto.AssignmentResponse;
import com.hrms.assignment.service.AssignmentService;
import com.hrms.attendance.model.*;
import com.hrms.attendance.repository.AttendanceRepository;
import com.hrms.common.exceptions.*;
import com.hrms.department.repository.DepartmentRepository;
import com.hrms.employee.model.Employee;
import com.hrms.holiday.repository.CompanyHolidayRepository;
import com.hrms.leave.model.LeaveType;
import com.hrms.leave.service.LeaveService;
import com.hrms.payroll.model.*;
import com.hrms.shift.model.Shift;
import com.hrms.shift.repository.ShiftRepository;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PayrollCalculationService {
    private final AssignmentService assignments; private final ShiftRepository shifts; private final CompanyHolidayRepository holidays;
    private final AttendanceRepository attendance; private final LeaveService leaves; private final DepartmentRepository departments;
    public PayrollCalculationService(AssignmentService assignments,ShiftRepository shifts,CompanyHolidayRepository holidays,
            AttendanceRepository attendance,LeaveService leaves,DepartmentRepository departments){this.assignments=assignments;this.shifts=shifts;
        this.holidays=holidays;this.attendance=attendance;this.leaves=leaves;this.departments=departments;}

    public PayrollRecord calculate(Employee employee,SalaryStructure salary,int month,int year,String actorId){
        YearMonth payrollMonth=YearMonth.of(year,month); LocalDate start=max(payrollMonth.atDay(1),employee.getJoiningDate());
        LocalDate end=employee.getTerminationDate()==null?payrollMonth.atEndOfMonth():min(payrollMonth.atEndOfMonth(),employee.getTerminationDate());
        if(end.isBefore(start))throw new ApiException(ErrorCode.VALIDATION_ERROR,HttpStatus.CONFLICT,"Employee is outside the payroll employment period");
        Set<LocalDate> holidayDates=holidays.findByDateBetweenAndActiveTrueOrderByDateAsc(start,end).stream().map(h->h.getDate()).collect(Collectors.toSet());
        PayrollCalculationDetails details=new PayrollCalculationDetails();details.setEmploymentPeriodStart(start);details.setEmploymentPeriodEnd(end);
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)){
            AssignmentResponse assignment=assignments.resolveHistorical(employee.getId(),date);
            Shift shift=shifts.findById(assignment.shiftId()).orElse(null);
            if(shift==null)throw new ResourceNotFoundException("Assigned shift was not found for "+date);
            if(!shift.getWorkingDays().contains(date.getDayOfWeek())){details.getWeeklyOffDates().add(date);continue;}
            if(holidayDates.contains(date)){details.getHolidayDates().add(date);continue;}
            details.getScheduledDates().add(date);
        }
        if(details.getScheduledDates().isEmpty())throw new ApiException(ErrorCode.CONFLICT,HttpStatus.CONFLICT,"Payroll period has no scheduled working days");
        Map<LocalDate,AttendanceRecord> records=attendance.findByEmployeeIdAndWorkDateBetween(employee.getId(),start,end).stream()
                .collect(Collectors.toMap(AttendanceRecord::getWorkDate,Function.identity(),(a,b)->a));
        records.values().stream().filter(this::unresolved).findFirst().ifPresent(record->{throw new ApiException(ErrorCode.INCOMPLETE_ATTENDANCE,
                HttpStatus.CONFLICT,"Unresolved attendance session exists on "+record.getWorkDate());});
        Map<LocalDate,LeaveType> approvedLeaves=leaves.approvedLeaveTypes(employee.getId(),start,end);
        for(LocalDate date:details.getScheduledDates()){
            AttendanceRecord record=records.get(date);
            if(record!=null&&hasCompletedSession(record)){details.getAttendedDates().add(date);continue;}
            LeaveType type=approvedLeaves.get(date);
            if(type!=null&&type.isPaid()){details.getPaidLeaveDates().add(date);continue;}
            if(type==LeaveType.LWP){details.getUnpaidLeaveDates().add(date);continue;}
            details.getUnexcusedAbsenceDates().add(date);
        }
        BigDecimal monthly=money(salary.getMonthlyFixedSalary());
        BigDecimal daily=monthly.divide(BigDecimal.valueOf(details.getScheduledDates().size()),2,RoundingMode.HALF_UP);
        int unpaidLeave=details.getUnpaidLeaveDates().size(),absence=details.getUnexcusedAbsenceDates().size(),totalUnpaid=unpaidLeave+absence;
        BigDecimal salaryDeduction=daily.multiply(BigDecimal.valueOf(totalUnpaid)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal net=monthly.subtract(salaryDeduction).setScale(2,RoundingMode.HALF_UP);
        if(net.signum()<0)throw new ConflictException("Payroll calculation produced a negative net salary");
        details.setFormula("monthlyFixedSalary - (dailySalaryRate x totalUnpaidDays) + earnings - otherDeductions");
        PayrollRecord p=new PayrollRecord();p.setId(new ObjectId().toHexString());p.setEmployeeId(employee.getId());
        p.setEmployeeCodeSnapshot(employee.getEmployeeCode());p.setEmployeeNameSnapshot((employee.getFirstName()+" "+employee.getLastName()).strip());
        p.setDepartmentIdSnapshot(employee.getDepartmentId());p.setDepartmentSnapshot(departmentName(employee.getDepartmentId()));
        p.setPayrollMonth(month);p.setPayrollYear(year);p.setMonthlyFixedSalarySnapshot(monthly);p.setScheduledWorkingDays(details.getScheduledDates().size());
        p.setAttendedDays(details.getAttendedDates().size());p.setPaidLeaveDays(details.getPaidLeaveDates().size());p.setUnpaidLeaveDays(unpaidLeave);
        p.setUnexcusedAbsenceDays(absence);p.setTotalUnpaidDays(totalUnpaid);p.setDailySalaryRate(daily);p.setSalaryDeduction(salaryDeduction);
        p.setTotalEarnings(zero());p.setTotalDeductions(salaryDeduction);p.setNetSalary(net);p.setStatus(PayrollStatus.DRAFT);
        p.setCalculationDetails(details);p.setCreatedBy(actorId);p.setUpdatedBy(actorId);return p;
    }
    private boolean unresolved(AttendanceRecord r){return r.getAttendanceStatus()==AttendanceStatus.INCOMPLETE||(r.getSessions()!=null&&r.getSessions().stream().anyMatch(s->s.getStatus()==SessionStatus.OPEN||s.getCheckInAt()==null||(s.getStatus()==SessionStatus.COMPLETED&&(s.getCheckOutAt()==null||!s.getCheckOutAt().isAfter(s.getCheckInAt())))));}
    private boolean hasCompletedSession(AttendanceRecord r){return r.getSessions()!=null&&r.getSessions().stream().anyMatch(s->s.getStatus()==SessionStatus.COMPLETED&&s.getCheckInAt()!=null&&s.getCheckOutAt()!=null&&s.getCheckOutAt().isAfter(s.getCheckInAt()));}
    private String departmentName(String id){if(id==null)return null;return departments.findById(id).map(d->d.getName()).orElse(id);}
    private static BigDecimal money(BigDecimal v){return v.setScale(2,RoundingMode.HALF_UP);} private static BigDecimal zero(){return BigDecimal.ZERO.setScale(2);}
    private static LocalDate max(LocalDate a,LocalDate b){return a.isAfter(b)?a:b;} private static LocalDate min(LocalDate a,LocalDate b){return a.isBefore(b)?a:b;}
}
