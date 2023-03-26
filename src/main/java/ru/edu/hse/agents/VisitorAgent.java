package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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
        addBehaviour(new ReceiveApproximateTimeBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("{0}, bye-bye", getAID().getLocalName()));
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

    private class ReceiveApproximateTimeBehaviour extends CyclicBehaviour {
        private static final String CONVERSATION_ID = "order-status";

        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    // double time = objectMapper.readValue(msg.getContent(), double.class);
                    Object[] data = objectMapper.readValue(msg.getContent(), Object[].class);
                    double time = (double) data[0];
                    int totalPrice = (int) data[1];
                    if (Math.abs(time) > 1e-9) {
                        logger.log(Level.INFO, MessageFormat.format("Approximate time of {0} is {1} minutes",
                                msg.getSender().getLocalName(), time));
                    } else {
                        logger.log(Level.INFO, MessageFormat.format("{0} got his order for {1} rub.!",
                                getLocalName(), totalPrice));
                        doDelete();
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            } else {
                block();
            }
        }
    }
}
