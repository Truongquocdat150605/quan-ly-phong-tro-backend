package com.example.quanliPT.model.enums;

public enum ServiceFrequency {
    MONTHLY("Hàng tháng"),
    WEEKLY("Hàng tuần"),
    DAILY("Hàng ngày"),
    ONE_TIME("Một lần"),
    QUARTERLY("Quý"),
    ANNUAL("Năm");

    private final String label;

    ServiceFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
