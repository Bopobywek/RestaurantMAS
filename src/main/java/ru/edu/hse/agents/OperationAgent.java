package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ru.edu.hse.models.OperationLogModel;
import ru.edu.hse.models.OperationModel;
import ru.edu.hse.models.OperationsLogModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class OperationAgent extends Agent {
    private static AtomicInteger index = new AtomicInteger(0);
    private OperationModel operation;
    public static final Queue<OperationLogModel> logModelQueue = new ConcurrentLinkedDeque<>();
    private final AID supervisorAID = new AID("SupervisorAgent", AID.ISLOCALNAME);
    private AID process;
    private OperationLogModel logModel = new OperationLogModel();
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.ORANGE, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    public void setup() {
        logModel.started = new Date();
        logger.log(Level.INFO, MessageFormat.format("Operation {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            process = (AID) args[0];
            operation = (OperationModel) args[1];
            logModel.process = (int) args[2];
            logModel.card = (int) args[3];
            logModel.id = index.getAndAdd(1);
        }
        addBehaviour(new MakeRequestBehaviour());
        addBehaviour(new FinishOperationBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Operation {0} terminated", getAID().getLocalName()));
    }

    private class MakeRequestBehaviour extends OneShotBehaviour {
        private static final String CONVERSATION_ID = "operation-reservation";

        public void action() {
            JsonMessage cfp = new JsonMessage(ACLMessage.REQUEST);
            cfp.addReceiver(supervisorAID);

            cfp.setContent(operation);
            cfp.setConversationId(CONVERSATION_ID);
            myAgent.send(cfp);
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
//                var mapper = new ObjectMapper();
//                try {
//                    logModel.cookerId = mapper.readValue(msg.getContent(), int.class);
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
//
//                JsonMessage cfp = new JsonMessage(ACLMessage.INFORM);
//                cfp.addReceiver(process);
//
//                cfp.setContent(operation.time);
//                cfp.setConversationId(CONVERSATION_ID);
//                myAgent.send(cfp);
//                logModel.ended = new Date();
//                // TODO: сохранить в лог запись
//                doDelete();
//            } else {
//                block();
//            }
//        }
//    }

    private class FinishOperationBehaviour extends CyclicBehaviour {
        private static final String CONVERSATION_ID = "operation-finish";
        private int step = 0;
        private static final ObjectMapper mapper = new ObjectMapper();

        public void action() {
            switch (step) {
                case 0 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("cook-finish"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        try {
                            logModel.cookerId = mapper.readValue(msg.getContent(), int.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("equip-finish"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        try {
                            logModel.equipId = mapper.readValue(msg.getContent(), int.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        step = 2;
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    JsonMessage cfp = new JsonMessage(ACLMessage.INFORM);
                    cfp.addReceiver(process);

                    cfp.setContent(new Object[] {logModel.id, operation.time});
                    cfp.setConversationId(CONVERSATION_ID);
                    myAgent.send(cfp);
                    logModel.ended = new Date();
                    logModel.isActive = false;
                    // TODO: сохранить в лог запись
                    logModelQueue.add(logModel);

                    try {
                        logger.log(Level.WARNING, mapper.writeValueAsString(logModel));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                    doDelete();
                }
            }
        }
    }
}
