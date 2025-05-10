package com.example.pricepk;

public class Product {
    private String id;
    private String name;
    private double pricePerKg;
    private double discountRate;
    private String description;

    public Product() {}

    public Product(String name, double pricePerKg, double discountRate, String description) {
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.discountRate = discountRate;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPricePerKg() {
        return pricePerKg;
    }

    public void setPricePerKg(double pricePerKg) {
        this.pricePerKg = pricePerKg;
    }

    public double getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(double discountRate) {
        this.discountRate = discountRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}