package ru.edu.hse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import ru.edu.hse.models.OperationModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

public class OperationAgent extends Agent {
    private OperationModel operation;
    private AID supervisor_aid = new AID("SupervisorAgent", AID.ISLOCALNAME);
    private AID process;
    private List<AID> cooks;
    private List<AID> equipments;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.ORANGE, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        logger.log(Level.INFO, MessageFormat.format("Operation {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            process = (AID) args[0];
            operation = (OperationModel) args[1];
        }
        addBehaviour(new MakeRequestBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Operation {0} terminated", getAID().getLocalName()));
    }

    private class MakeRequestBehaviour extends OneShotBehaviour {
        private static final String CONVERSATION_ID = "operation-reservation";

        public void action() {
            JsonMessage cfp = new JsonMessage(ACLMessage.REQUEST);
            cfp.addReceiver(supervisor_aid);

            cfp.setContent(operation);
            cfp.setConversationId(CONVERSATION_ID);
            myAgent.send(cfp);
        }
    }
}
