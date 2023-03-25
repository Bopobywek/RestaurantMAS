package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EquipmentCollectionModel(@JsonProperty("equipment") EquipmentModel[] equipmentModels) {
}
