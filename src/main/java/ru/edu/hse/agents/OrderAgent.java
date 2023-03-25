package ru.edu.hse.agents;

import jade.core.Agent;
import jade.util.Logger;

import java.text.MessageFormat;
import java.util.logging.Level;

public class OrderAgent extends Agent {
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());

    @Override
    protected void setup() {
        logger.log(Level.INFO, MessageFormat.format("Order {0} is created.", getAID().getLocalName()));
    }


}
