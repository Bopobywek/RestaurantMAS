package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuDishModel {
    @JsonProperty("menu_dish_id")
    public int id;
    @JsonProperty("menu_dish_card")
    public int card;
    @JsonProperty("menu_dish_price")
    public int price;
    @JsonProperty("menu_dish_active")
    public boolean isActive;
}
