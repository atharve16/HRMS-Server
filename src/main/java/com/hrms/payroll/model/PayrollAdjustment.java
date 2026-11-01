package com.hrms.payroll.model;
import java.math.BigDecimal;
import java.time.Instant;

public class PayrollAdjustment {
    private PayrollAdjustmentType type;
    private String description;
    private BigDecimal amount;
    private String createdBy;
    private Instant createdAt;
    public PayrollAdjustmentType getType(){return type;} public void setType(PayrollAdjustmentType v){type=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}
