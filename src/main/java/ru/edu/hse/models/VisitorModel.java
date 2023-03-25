package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class VisitorModel {
    @JsonProperty("vis_name")
    public String name;
    @JsonProperty("vis_ord_started")
    public Date orderStarted;
    @JsonProperty("vis_ord_ended")
    public Date orderEnded;
    @JsonProperty("vis_ord_total")
    public Integer orderTotal;
    @JsonProperty("vis_ord_dishes")
    public List<DishModel> dishList;
}
