package model;

import java.util.ArrayList;
import java.util.List;

public class Supplier {
    private int supplierId;
    private String name;
    private String email;
    private String address;
    private String taxCode;
    private List<String> phoneNumbers = new ArrayList<>();

    public Supplier() {
    }

    public Supplier(int supplierId, String name, String email, String address, String taxCode) {
        this.supplierId = supplierId;
        this.name = name;
        this.email = email;
        this.address = address;
        this.taxCode = taxCode;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers != null ? phoneNumbers : new ArrayList<>();
    }
}
