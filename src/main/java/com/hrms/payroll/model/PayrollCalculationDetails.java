package com.hrms.payroll.model;
import java.time.LocalDate;
import java.util.*;

public class PayrollCalculationDetails {
    private LocalDate employmentPeriodStart;
    private LocalDate employmentPeriodEnd;
    private List<LocalDate> scheduledDates=new ArrayList<>();
    private List<LocalDate> attendedDates=new ArrayList<>();
    private List<LocalDate> paidLeaveDates=new ArrayList<>();
    private List<LocalDate> unpaidLeaveDates=new ArrayList<>();
    private List<LocalDate> unexcusedAbsenceDates=new ArrayList<>();
    private List<LocalDate> holidayDates=new ArrayList<>();
    private List<LocalDate> weeklyOffDates=new ArrayList<>();
    private String formula;
    public LocalDate getEmploymentPeriodStart(){return employmentPeriodStart;} public void setEmploymentPeriodStart(LocalDate v){employmentPeriodStart=v;}
    public LocalDate getEmploymentPeriodEnd(){return employmentPeriodEnd;} public void setEmploymentPeriodEnd(LocalDate v){employmentPeriodEnd=v;}
    public List<LocalDate> getScheduledDates(){return scheduledDates;} public void setScheduledDates(List<LocalDate> v){scheduledDates=v;}
    public List<LocalDate> getAttendedDates(){return attendedDates;} public void setAttendedDates(List<LocalDate> v){attendedDates=v;}
    public List<LocalDate> getPaidLeaveDates(){return paidLeaveDates;} public void setPaidLeaveDates(List<LocalDate> v){paidLeaveDates=v;}
    public List<LocalDate> getUnpaidLeaveDates(){return unpaidLeaveDates;} public void setUnpaidLeaveDates(List<LocalDate> v){unpaidLeaveDates=v;}
    public List<LocalDate> getUnexcusedAbsenceDates(){return unexcusedAbsenceDates;} public void setUnexcusedAbsenceDates(List<LocalDate> v){unexcusedAbsenceDates=v;}
    public List<LocalDate> getHolidayDates(){return holidayDates;} public void setHolidayDates(List<LocalDate> v){holidayDates=v;}
    public List<LocalDate> getWeeklyOffDates(){return weeklyOffDates;} public void setWeeklyOffDates(List<LocalDate> v){weeklyOffDates=v;}
    public String getFormula(){return formula;} public void setFormula(String v){formula=v;}
}
