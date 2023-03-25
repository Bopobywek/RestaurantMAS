package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DishCardModel {
    @JsonProperty("card_id")
    public int id;
    @JsonProperty("dish_name")
    public String name;
    @JsonProperty("card_descr")
    public String description;
    @JsonProperty("card_time")
    public double time;
    @JsonProperty("operations")
    public List<OperationModel> operations;
}
