package ru.edu.hse.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
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
import java.util.logging.Level;

public class VisitorAgent extends Agent {
    private VisitorModel visitorData;
    private AID supervisor_aid = new AID("SupervisorAgent", AID.ISLOCALNAME);
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.CYAN, jade.util.Logger.getMyLogger(this.getClass().getName()));


    @Override
    protected void setup() {
        logger.log(Level.INFO, MessageFormat.format("Visitor {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            visitorData = (VisitorModel) args[0];

            System.out.println(MessageFormat.format("Found supervisor: {0}.",
                            supervisor_aid));
//            DFAgentDescription template = new DFAgentDescription();
//            ServiceDescription serviceDescription = new ServiceDescription();
//            serviceDescription.setType("order-supervising");
//            template.addServices(serviceDescription);
//            try {
//                DFAgentDescription[] result = DFService.search(this, template);
//                if (result.length != 0) {
//                    supervisor_aid = result[0].getName();
//                    System.out.println(MessageFormat.format("Found supervisor: {0}.",
//                            supervisor_aid));
//                }
//            } catch (FIPAException e) {
//                e.printStackTrace();
//            }

            addBehaviour(new MakeOrderBehaviour());
        } else {
            System.out.println(MessageFormat.format("No information about visitor {0} specified",
                    getAID().getName()));
            doDelete();
        }
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
