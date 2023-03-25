package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OperationModel {
    @JsonProperty("oper_type")
    public int type;
    @JsonProperty("oper_time")
    public double time;
    @JsonProperty("oper_async_point")
    public int asyncPoint;
    @JsonProperty("oper_products")
    public List<OperationProductModel> productModels;
}
