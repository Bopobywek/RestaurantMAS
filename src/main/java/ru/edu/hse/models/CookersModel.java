package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CookersModel {
    @JsonProperty("cookers")
    public List<CookModel> cookers;
}
