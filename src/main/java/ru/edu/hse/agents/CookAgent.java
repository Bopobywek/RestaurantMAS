package ru.edu.hse.agents;

import jade.core.AID;
import jade.core.Agent;
import ru.edu.hse.models.CookModel;
import ru.edu.hse.models.CookersModel;
import ru.edu.hse.models.OperationModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.logging.Level;

public class CookAgent extends Agent {
    private CookModel cook;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.PINK, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        logger.log(Level.INFO, MessageFormat.format("Cook {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            cook = (CookModel) args[0];
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Cook {0} terminated", getAID().getLocalName()));
    }
}
