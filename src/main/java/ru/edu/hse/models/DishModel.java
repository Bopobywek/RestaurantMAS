package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DishModel {
    @JsonProperty("ord_dish_id")
    public Integer id;
    @JsonProperty("menu_dish")
    public Integer menuId;
}
