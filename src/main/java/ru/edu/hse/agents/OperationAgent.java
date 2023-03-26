package ru.edu.hse.agents;

import jade.core.AID;
import jade.core.Agent;
import ru.edu.hse.models.DishCardModel;
import ru.edu.hse.models.OperationModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.logging.Level;

public class OperationAgent extends Agent {
    private OperationModel operation;
    private AID process;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.ORANGE, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        logger.log(Level.INFO, MessageFormat.format("Operation {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            process = (AID) args[0];
            operation = (OperationModel) args[1];
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Operation {0} terminated", getAID().getLocalName()));
    }
}
