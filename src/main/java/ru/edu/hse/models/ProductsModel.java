package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProductsModel(@JsonProperty("products") ProductModel[] productsModels) {
}
