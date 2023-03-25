package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import ru.edu.hse.models.ProductModel;
import ru.edu.hse.models.VisitorModel;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class WarehouseAgent extends Agent {
    private List<ProductModel> products;
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            products = Arrays.stream((ProductModel[]) args).toList();

            var mapper = new ObjectMapper();
            for (var product : products) {
                try {
                    logger.log(Level.INFO, mapper.writeValueAsString(product));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

            addBehaviour(new OrderServerBehaviour());
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Warehouse-agent " + getAID().getName() + " terminating.");
    }


    private class OrderServerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("warehouse-get"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                logger.log(Level.INFO, "Warehouse received request");
                var mapper = new ObjectMapper();
                ACLMessage reply = msg.createReply();
                try {
                    reply.setContent(mapper.writeValueAsString(products));
                    reply.setPerformative(ACLMessage.INFORM);
                    myAgent.send(reply);
                    logger.log(Level.INFO, "Warehouse send reply");
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                // new TypeReference<List<ProductModel>>(){}
            } else {
                block();
            }
        }
    }

}
