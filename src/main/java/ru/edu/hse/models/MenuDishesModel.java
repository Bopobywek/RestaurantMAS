package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MenuDishesModel {
    @JsonProperty("menu_dishes")
    public MenuDishModel[] menuDishModels;
}
