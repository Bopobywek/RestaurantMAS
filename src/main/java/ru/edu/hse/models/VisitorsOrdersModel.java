package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VisitorsOrdersModel(@JsonProperty("visitors_orders") VisitorModel[] visitorModels) {
}
