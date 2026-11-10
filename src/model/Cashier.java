package model;

import java.time.LocalDate;

public class Cashier extends Employee {
    private int managerId;
    private int assignedCounter;

    public Cashier() {
        super();
    }

    public Cashier(int employeeId, String username, String firstName, String lastName,
                LocalDate startDate, LocalDate dateOfBirth, String address,
                int managerId, int assignedCounter) {
        super(employeeId, username, firstName, lastName, startDate, dateOfBirth, address);
        this.managerId = managerId;
        this.assignedCounter = assignedCounter;
    }

    @Override
    public String getRole() {
        return "CASHIER";
    }

    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public int getAssignedCounter() {
        return assignedCounter;
    }

    public void setAssignedCounter(int assignedCounter) {
        this.assignedCounter = assignedCounter;
    }
}
