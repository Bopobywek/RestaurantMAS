package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DishCardsModel {
    @JsonProperty("dish_cards")
    public DishCardModel[] dishCardModels;
}
