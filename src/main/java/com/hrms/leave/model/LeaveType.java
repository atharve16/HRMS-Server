package com.hrms.leave.model;

public enum LeaveType {
    CL("Casual Leave", true),
    SL("Sick Leave", true),
    EL("Earned Leave", true),
    LWP("Leave Without Pay", false),
    CO("Compensatory Off", true);

    private final String displayName;
    private final boolean paid;

    LeaveType(String displayName, boolean paid) {
        this.displayName = displayName;
        this.paid = paid;
    }

    public String getDisplayName() { return displayName; }
    public boolean isPaid() { return paid; }
}
