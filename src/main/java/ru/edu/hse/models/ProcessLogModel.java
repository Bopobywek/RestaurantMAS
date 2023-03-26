package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

public class ProcessLogModel{
    @JsonProperty("proc_id")
    public int id;
    @JsonProperty("ord_dish")
    public int dish;
    @JsonProperty("proc_started")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date started;
    @JsonProperty("proc_ended")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date ended;
    @JsonProperty("proc_active")
    public boolean isActive;
    @JsonProperty("proc_operations")
    public List<ProcessOperationLogModel> operations;
}

