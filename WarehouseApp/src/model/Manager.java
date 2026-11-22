package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Manager extends Employee {
    private BigDecimal bonus;

    public Manager() {
        super();
    }

    public Manager(int employeeId, String username, String firstName, String lastName, LocalDate startDate, LocalDate dateOfBirth, String address, BigDecimal bonus) {
        super(employeeId, username, firstName, lastName, startDate, dateOfBirth, address);
        this.bonus = bonus;
    }

    @Override
    public String getRole() {
        return "MANAGER";
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(BigDecimal bonus) {
        this.bonus = bonus;
    }
}
