package com.example.quanliPT.model.enums;

public enum ServiceUnit {
    UNIT("Bộ"),
    PERSON("Người"),
    MONTH("Tháng"),
    WEEK("Tuần"),
    DAY("Ngày"),
    KWH("kWh"),
    CUBIC_METER("m³"),
    LITER("Lít"),
    HOUR("Giờ"),
    TIME("Lần");

    private final String label;

    ServiceUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
