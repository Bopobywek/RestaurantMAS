package ru.edu.hse.models;

public class ReservationModel {
    public MenuDishModel menuDishModel;
    public DishCardModel dishCardModel;
    public Boolean isReserved = false;

    public ReservationModel() {}

    public ReservationModel(MenuDishModel menuDishModel, DishCardModel dishCardModel) {
        this.menuDishModel = menuDishModel;
        this.dishCardModel = dishCardModel;
    }
}
