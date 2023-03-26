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
    private final Queue<OperationModel> operations = new ArrayDeque<>();
    private DishCardModel card;
    private AID order;
    private int totalOperations;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.YELLOW, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {

        var args = getArguments();
        if (args != null) {
            order = (AID) args[0];
            card = (DishCardModel) args[1];
            operations.addAll(card.operations);
            totalOperations = operations.size();
            logger.log(Level.INFO, MessageFormat.format("Process {0} is created with {1} operations.", getAID().getLocalName(), totalOperations));
            addBehaviour(new OperationServerBehaviour());
//            addBehaviour(new FinishOperationBehaviour());
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
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("operation-finish"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        --totalOperations;
                        ACLMessage orderMessage = new ACLMessage(ACLMessage.INFORM);
                        orderMessage.setContent(msg.getContent());
                        orderMessage.setConversationId("operation-finish");
                        orderMessage.addReceiver(order);
                        send(orderMessage);
                        step = 0;
                        if(totalOperations == 0) {
                            doDelete();
                        }
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

//    private class FinishOperationBehaviour extends CyclicBehaviour {
//        private static final String CONVERSATION_ID = "operation-finish";
//
//        public void action() {
//            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
//                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
//            ACLMessage msg = myAgent.receive(messageTemplate);
//            if (msg != null) {
//                --totalOperations;
//                ACLMessage orderMessage = new ACLMessage(ACLMessage.INFORM);
//                orderMessage.setContent(msg.getContent());
//                orderMessage.setConversationId(CONVERSATION_ID);
//                orderMessage.addReceiver(order);
//                send(orderMessage);
//
//                if(totalOperations == 0) {
//                    doDelete();
//                }
//            } else {
//                block();
//            }
//        }
//    }
}
