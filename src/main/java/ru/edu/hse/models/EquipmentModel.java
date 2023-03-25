package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EquipmentModel {
    @JsonProperty("equip_type")
    public int type;
    @JsonProperty("equip_name")
    public String name;
    @JsonProperty("equip_active")
    public boolean isActive;
}
