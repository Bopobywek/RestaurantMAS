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
import ru.edu.hse.models.EquipmentModel;
import ru.edu.hse.models.OperationExecutionModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.logging.Level;

public class EquipmentAgent extends Agent {
    private EquipmentModel equipment;
    private final ColorfulLogger logger =
            new ColorfulLogger(DebugColor.SAND, jade.util.Logger.getMyLogger(this.getClass().getName()));
    private boolean isVacant;

    @Override
    protected void setup() {
        isVacant = true;
        Object[] args = getArguments();
        if (args != null) {
            equipment = (EquipmentModel) getArguments()[0];

        }
        logger.log(Level.INFO, MessageFormat.format("{0} equipment loaded", equipment.name));

        var agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        var serviceDescription = new ServiceDescription();
        serviceDescription.setType("equipment-service");
        serviceDescription.setName("JADE-equipment-service");
        agentDescription.addServices(serviceDescription);
        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException exception) {
            exception.printStackTrace();
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

        System.out.println("Equipment-agent " + equipment.name + " terminating.");
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
            cfp.setContent(equipment.id);
            cfp.setConversationId("equip-finish");
            logger.log(Level.INFO, MessageFormat.format("{0} finish work and sent to operation signal",
                    myAgent.getLocalName()));
            myAgent.send(cfp);
        }
    }

    private class OperationTakerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplateEquipment = MessageTemplate.and(MessageTemplate.MatchConversationId("equipment-start"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msgCook = myAgent.receive(messageTemplateEquipment);

            if (msgCook != null) {
                var mapper = new ObjectMapper();
                try {
                    var operation = mapper.readValue(msgCook.getContent(), OperationExecutionModel.class);
                    isVacant = false;
                    addBehaviour(new WakerBehaviour(myAgent, ((int) operation.time * 1000L) / 900) {
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
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("equipment-reservation"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                var reply = msg.createReply(ACLMessage.INFORM);
                int type;
                var mapper = new ObjectMapper();
                try {
                    type = mapper.readValue(msg.getContent(), int.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                if (isVacant && equipment.type == type) {
                    try {
                        reply.setContent(mapper.writeValueAsString(true));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        reply.setContent(mapper.writeValueAsString(false));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
}
