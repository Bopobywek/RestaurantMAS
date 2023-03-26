package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import ru.edu.hse.models.*;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ProcessAgent extends Agent {
    private static final AtomicInteger index = new AtomicInteger(0);
    private int id;
    private final Queue<OperationModel> operations = new ArrayDeque<>();
    public static final Queue<ProcessLogModel> logModelQueue = new ConcurrentLinkedDeque<>();
    public final ProcessLogModel logModel = new ProcessLogModel();
    private DishCardModel card;
    private AID order;
    private int totalOperations;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.YELLOW, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        id = index.getAndAdd(1);
        logModel.started = new Date();
        logModel.operations = new ArrayList<>();
        logModel.id = id;
        var args = getArguments();
        if (args != null) {
            order = (AID) args[0];
            card = (DishCardModel) args[1];
            logModel.dish = card.id;
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
                            createOperation(operation, index, id);
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
                        var orderMessage = new JsonMessage(ACLMessage.INFORM);

                        var mapper = new ObjectMapper();
                        Object[] data;
                        try {
                             data = mapper.readValue(msg.getContent(), Object[].class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        var processOperationLogModel = new ProcessOperationLogModel();
                        processOperationLogModel.operId = (int) data[0];
                        logModel.operations.add(processOperationLogModel);
                        orderMessage.setContent((double) data[1]); // time
                        orderMessage.setConversationId("operation-finish");
                        orderMessage.addReceiver(order);
                        send(orderMessage);
                        step = 0;
                        if (totalOperations == 0) {
                            logModel.ended = new Date();
                            logModel.isActive = false;
                            logModelQueue.add(logModel);
                            doDelete();
                        }
                    } else {
                        block();
                    }
                }
            }
        }

        // addOperationsWaiter(operationsCount)

        @Override
        public boolean done() {
            return operations.isEmpty() && step == 0;
        }

        private void createOperation(OperationModel operationModel, int index, int process_id) {
            AgentContainer container = getContainerController();
            try {
                container.createNewAgent(MessageFormat.format("OperationAgent{0}$$${1}", index, myAgent.getLocalName()),
                        OperationAgent.class.getName(), new Object[]{myAgent.getAID(), operationModel, process_id, card.id}).start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
