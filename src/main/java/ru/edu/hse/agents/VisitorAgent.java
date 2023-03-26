package ru.edu.hse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import ru.edu.hse.configuration.JadeAgent;
import ru.edu.hse.models.VisitorModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.logging.Level;

public class VisitorAgent extends Agent {
    private VisitorModel visitorData;
    private AID supervisor_aid = new AID("SupervisorAgent", AID.ISLOCALNAME);
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.CYAN, jade.util.Logger.getMyLogger(this.getClass().getName()));


    @Override
    protected void setup() {
        var args = getArguments();
        if (args != null) {
            visitorData = (VisitorModel) args[0];
            Date start = new Date(visitorData.orderStarted.getYear(), visitorData.orderStarted.getMonth(), visitorData.orderStarted.getDate(), 13, 0);
            this.doWait((visitorData.orderStarted.getTime() - start.getTime()) / 900);
            addBehaviour(new MakeOrderBehaviour());
        } else {
            System.out.println(MessageFormat.format("No information about visitor {0} specified",
                    getAID().getName()));
            doDelete();
        }
        logger.log(Level.INFO, MessageFormat.format("Visitor {0} is created.", getAID().getLocalName()));
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("{0}, bye-bye", getAID().getName()));
    }

    private class MakeOrderBehaviour extends OneShotBehaviour {
        private static final String CONVERSATION_ID = "order-placing";

        public void action() {
            JsonMessage cfp = new JsonMessage(ACLMessage.REQUEST);
            cfp.addReceiver(supervisor_aid);

            cfp.setContent(visitorData.dishList);
            cfp.setConversationId(CONVERSATION_ID);
            myAgent.send(cfp);
        }
    }
}
