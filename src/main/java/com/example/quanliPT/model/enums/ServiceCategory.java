package com.example.quanliPT.model.enums;

public enum ServiceCategory {
    WATER("Nước"),
    ELECTRICITY("Điện"),
    GARBAGE("Rác"),
    INTERNET("Internet"),
    SECURITY("An ninh"),
    CLEANING("Vệ sinh"),
    MAINTENANCE("Bảo trì"),
    OTHER("Khác");

    private final String label;

    ServiceCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
