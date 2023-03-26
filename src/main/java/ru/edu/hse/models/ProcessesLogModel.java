package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ProcessesLogModel {
    @JsonProperty("process_log")
    public List<ProcessLogModel> processesLog;
}
