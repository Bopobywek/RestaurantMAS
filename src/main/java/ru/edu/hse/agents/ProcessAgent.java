package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import ru.edu.hse.models.DishCardModel;
import ru.edu.hse.models.OperationModel;
import ru.edu.hse.models.VisitorModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.logging.Level;

public class ProcessAgent extends Agent {
    private static final Queue<OperationModel> operations = new ArrayDeque<>();
    private DishCardModel card;
    private AID order;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.YELLOW, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        logger.log(Level.INFO, MessageFormat.format("Process {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            order = (AID) args[0];
            card = (DishCardModel) args[1];
            operations.addAll(card.operations);
            addBehaviour(new OperationServerBehaviour());
        }
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Process {0} terminated", getAID().getLocalName()));
    }

    private class OperationServerBehaviour extends Behaviour {
        private int step = 0;
        private int index = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    if (!operations.isEmpty()) {
                        var operation = operations.poll();
                        if (operation != null) {
                            createOperation(operation, index);
                            ++index;
                            step = 1;
                        }

                    } else {
                        block();
                    }
                }
                case 1 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("operation-status"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        logger.log(Level.INFO, MessageFormat.format("{0} is completed", msg.getSender().getLocalName()));
                        step = 0;
                    } else {
                        block();
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return operations.isEmpty() && step == 0;
        }

        private void createOperation(OperationModel operationModel, int index) {
            AgentContainer container = getContainerController();
            try {
                container.createNewAgent(MessageFormat.format("OperationAgent{0}$$${1}", index, myAgent.getLocalName()),
                        OperationAgent.class.getName(), new Object[]{myAgent.getAID(), operationModel}).start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class OrderReceiveBehavoiur extends CyclicBehaviour {

        @Override
        public void action() {
            /*var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("warehouse-get"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                logger.log(Level.INFO, "Warehouse received request from menu");
                var mapper = new ObjectMapper();
                ACLMessage reply = msg.createReply();
                try {
                    reply.setContent(mapper.writeValueAsString(products));
                    reply.setPerformative(ACLMessage.INFORM);
                    myAgent.send(reply);
                    logger.log(Level.INFO, "Warehouse send reply to menu");
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                // new TypeReference<List<ProductModel>>(){}
            } else {
                block();
            }*/
        }
    }
}
