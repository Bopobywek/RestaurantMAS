package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CookModel {
    @JsonProperty("cook_id")
    public int id;
    @JsonProperty("cook_name")
    public String name;
    @JsonProperty("cook_active")
    public boolean isActive;
}
