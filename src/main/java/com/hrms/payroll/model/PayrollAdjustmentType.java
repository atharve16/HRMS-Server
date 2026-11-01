package com.hrms.payroll.model;
public enum PayrollAdjustmentType {
    BONUS(true), OVERTIME_PAY(true), OTHER_EARNING(true), OTHER_DEDUCTION(false);
    private final boolean earning;
    PayrollAdjustmentType(boolean earning){this.earning=earning;}
    public boolean isEarning(){return earning;}
}
