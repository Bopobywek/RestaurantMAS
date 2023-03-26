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
import ru.edu.hse.models.CookModel;
import ru.edu.hse.models.OperationExecutionModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.logging.Level;

public class CookAgent extends Agent {
    private CookModel cook;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.PINK, jade.util.Logger.getMyLogger(this.getClass().getName()));
    private boolean isVacant = true;

    @Override
    public void setup() {
        isVacant = true;
        logger.log(Level.INFO, MessageFormat.format("Cook {0} is created.", getAID().getLocalName()));
        var agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        var serviceDescription = new ServiceDescription();
        serviceDescription.setType("cook-service");
        serviceDescription.setName("JADE-cook-service");
        agentDescription.addServices(serviceDescription);
        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException exception) {
            exception.printStackTrace();
        }

        var args = getArguments();
        if (args != null) {
            cook = (CookModel) args[0];
        }

        addBehaviour(new OperationServerBehaviour());
        addBehaviour(new OperationTakerBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(MessageFormat.format("Cook {0} terminated", getAID().getLocalName()));
    }

    private class SendOperationSignalBehaviour extends OneShotBehaviour {
        private final AID operationAID;

        public SendOperationSignalBehaviour(String operationName) {
            this.operationAID = new AID(operationName, AID.ISLOCALNAME);
        }
        @Override
        public void action() {
            var cfp = new JsonMessage(ACLMessage.INFORM);
            cfp.addReceiver(operationAID);
            cfp.setContent(cook.id);
            cfp.setConversationId("cook-finish");
            logger.log(Level.INFO, MessageFormat.format("{0} finish work and sent to operation signal", myAgent.getLocalName()));
            myAgent.send(cfp);
        }
    }

    private class OperationTakerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplateCook = MessageTemplate.and(MessageTemplate.MatchConversationId("cook-start"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msgCook = myAgent.receive(messageTemplateCook);

            if (msgCook != null) {
                var mapper = new ObjectMapper();
                try {
                    var operation = mapper.readValue(msgCook.getContent(), OperationExecutionModel.class);
                    isVacant = false;
                    addBehaviour(new WakerBehaviour(myAgent, ((int)operation.time * 1000L) / 900) {
                        @Override
                        protected void onWake() {
                            isVacant = true;
                            // Отправляем уведомление операции
                            addBehaviour(new SendOperationSignalBehaviour(operation.operationName));
                        }
                    });

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                block();
            }
        }
    }

    private class OperationServerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("cook-reservation"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                var mapper = new ObjectMapper();
                var reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                try {
                    reply.setContent(mapper.writeValueAsString(isVacant));
                    myAgent.send(reply);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            } else {
                block();
            }
        }
    }

}
