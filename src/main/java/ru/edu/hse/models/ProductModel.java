package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class ProductModel {
    @JsonProperty("prod_item_id")
    public int id;
    @JsonProperty("prod_item_type")
    public int type;
    @JsonProperty("prod_item_name")
    public String name;
    @JsonProperty("prod_item_company")
    public String company;
    @JsonProperty("prod_item_unit")
    public String unit;
    @JsonProperty("prod_item_quantity")
    public double quantity;
    @JsonProperty("prod_item_cost")
    public double cost;
    @JsonProperty("prod_item_delivered")
    public Date delivered;
    @JsonProperty("prod_item_valid_until")
    public Date validUntil;
}
