package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import ru.edu.hse.configuration.JadeAgent;
import ru.edu.hse.models.MenuDishModel;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

@JadeAgent
public class SupervisorAgent extends Agent {
    private static List<MenuDishModel> menuItems;
    private static Queue<ACLMessage> visitors = new ArrayDeque<>();
    private static final AID menu = new AID("MenuAgent", AID.ISLOCALNAME);
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());


    @Override
    protected void setup() {
        var agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());
        var serviceDescription = new ServiceDescription();
        serviceDescription.setType("order-supervising");
        serviceDescription.setName("JADE-order-supervising");
        agentDescription.addServices(serviceDescription);
        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException exception) {
            exception.printStackTrace();
        }

        addBehaviour(new ReceiveOrderBehaviour());
        addBehaviour(new OrderServerBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Supervisor-agent " + getAID().getName() + " terminating.");
    }

    private class ReceiveOrderBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("order-placing"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                logger.log(Level.INFO, "Supervisor received order: " + msg.getContent());
                visitors.add(msg);
            } else {
                block();
            }
        }
    }



    private class OrderServerBehaviour extends CyclicBehaviour {
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    if (!visitors.isEmpty()) {
                        ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
                        cfp.addReceiver(menu);
                        cfp.setConversationId("menu-get");
                        myAgent.send(cfp);
                        logger.log(Level.INFO, "Supervisor send request to MenuAgent");
                        step = 1;
                    }
                }
                case 1 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("menu-get"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        logger.log(Level.INFO, "Supervisor received menu: " + msg.getContent());
                        var mapper = new ObjectMapper();
                        try {
                            menuItems = mapper.readValue(msg.getContent(), new TypeReference<>() {
                            });
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        // TODO: создать заказ
                        var visitorOrder = visitors.remove();
                        logger.log(Level.INFO, "Supervisor creates order for " + visitorOrder.getSender().getLocalName());
                        step = 0;
                    } else {
                        block();
                    }
                }
            }
        }

    }
}
