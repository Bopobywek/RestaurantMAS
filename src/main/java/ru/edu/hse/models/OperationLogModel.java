package ru.edu.hse.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class OperationLogModel{
    @JsonProperty("oper_id")
    public int id;
    @JsonProperty("oper_proc")
    public int process;
    @JsonProperty("oper_card")
    public int card;
    @JsonProperty("oper_started")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date started;
    @JsonProperty("oper_ended")
    @JsonFormat(shape= JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss", timezone="GMT")
    public Date ended;
    @JsonProperty("oper_equip_id")
    public int equipId;
    @JsonProperty("oper_coocker_id")
    public int cookerId;
    @JsonProperty("oper_active")
    public boolean isActive;
}
















