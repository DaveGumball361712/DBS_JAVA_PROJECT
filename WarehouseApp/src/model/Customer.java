package model;

import java.time.LocalDate;

public class Customer {
    private int customerId;
    private String name;
    private String email;
    private int points;
    private String phoneNumber;
    private LocalDate registerDate;

    public Customer() {
    }

    public Customer(int customerId, String name, String email, int points,
                    String phoneNumber, LocalDate registerDate) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.points = points;
        this.phoneNumber = phoneNumber;
        this.registerDate = registerDate;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDate registerDate) {
        this.registerDate = registerDate;
    }
}