package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OperationProductModel {
    @JsonProperty("prod_type")
    public int type;
    @JsonProperty("prod_quantity")
    public double quantity;
}
