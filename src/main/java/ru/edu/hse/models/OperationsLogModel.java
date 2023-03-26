package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OperationsLogModel {
    @JsonProperty("operation_log")
    public List<OperationLogModel> log;
}
