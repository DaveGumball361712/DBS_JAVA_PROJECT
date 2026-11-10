package model;

import java.time.LocalDate;

public class WarehouseStaff extends Employee {
    private int managerId;
    private String assignedZone;

    public WarehouseStaff() {
        super();
    }

    public WarehouseStaff(int employeeId, String username, String firstName, String lastName,
                        LocalDate startDate, LocalDate dateOfBirth, String address,
                        int managerId, String assignedZone) {
        super(employeeId, username, firstName, lastName, startDate, dateOfBirth, address);
        this.managerId = managerId;
        this.assignedZone = assignedZone;
    }

    @Override
    public String getRole() {
        return "WAREHOUSE_STAFF";
    }

    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public String getAssignedZone() {
        return assignedZone;
    }

    public void setAssignedZone(String assignedZone) {
        this.assignedZone = assignedZone;
    }
}
