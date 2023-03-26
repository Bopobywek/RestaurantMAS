package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
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

    @JsonIgnore
    public HashMap<Integer, Double> getQuantityForProducts() {
        HashMap<Integer, Double> productsQuantity = new HashMap<>();
        for (var operation : operations) {
            for (var productInOperation : operation.productModels) {
                if (productsQuantity.containsKey(productInOperation.type)) {
                    double was = productsQuantity.get(productInOperation.type);
                    productsQuantity.put(productInOperation.type, was + productInOperation.quantity);
                } else {
                    productsQuantity.put(productInOperation.type, productInOperation.quantity);
                }
            }
        }

        return productsQuantity;
    }
}
