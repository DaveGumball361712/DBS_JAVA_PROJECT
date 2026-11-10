package model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.ListResourceBundle;

public abstract class Employee {
    protected int employeeId;
    protected String username;
    protected String firstName;
    protected String lastName;
    protected LocalDate startDate;
    protected LocalDate dateOfBirth;
    protected String address;
    protected List<String> phoneNUmbers = new ArrayList<>();
    public Employee(){
    }

    public Employee(int employeeId, String username, String firstName, String lastName, LocalDate startDate, LocalDate dateOfBirth, String address) {
        this.employeeId = employeeId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.startDate = startDate;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
    }

    public getFullName(){
        return (lastName != null ? lastName.trim() : "") + " " + (firstName != null ? firstName.trim() : "");
    }

    public int getAge() {
        if (this.dateOfBirth == null) {
            return 0;
        }
        return Period.between(this.dateOfBirth, localDate.now()).getYears(); 
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName(){
        return firstName;
    }

    public void setFirstName(String firstName){
        this.firstName = firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public void setLastName(String lastName){
        this.lastName = lastName;
    }

    public LocalDate getStartdDate(){
        return startDate;
    }

    public void setStartDate(LocalDate startDate){
        this.startDate = startDate;
    }

    public LocalDate getDateOfBirth(){
        return dateOfBirth;
    }
    
    public void setDateOfBirth(LocalDate dateOfBirth){
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress(){
        return address;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public List<String> getPhoneNumbers(){
        return phoneNUmbers;
    }

    public void setPhoneNumbers(List<String> phoneNumber){
        this.phoneNUmbers = phoneNumbes != null ? phoneNUmbers : new ArrayList<>();
    }

    public void addPhoneNumber(String phone){
        if (phone != null && !phone.isBlank() && !this.phoneNUmbers.contain(phone)) {
            this.phoneNUmbers.add(phone);
        }
    }
}
