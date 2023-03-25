package ru.edu.hse.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ru.edu.hse.configuration.JadeAgent;

import java.util.ArrayDeque;
import java.util.Queue;

@JadeAgent
public class SupervisorAgent extends Agent {
    private static final Queue<OrderAgent> orders = new ArrayDeque<>();

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

        addBehaviour(new OrderHandleBehaviour());
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

    private static class OrderHandleBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("order-placing"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                System.out.println("Supervisor received: " + msg.getContent());
                ///////////////////
                // checkMenu
                // createOrder
            } else {
                block();
            }
        }
    }
}
