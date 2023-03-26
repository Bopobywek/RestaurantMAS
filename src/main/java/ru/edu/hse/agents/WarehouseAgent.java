package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ru.edu.hse.models.ProductModel;
import ru.edu.hse.models.ReservationModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.util.*;
import java.util.logging.Level;

public class WarehouseAgent extends Agent {
    private static final Queue<ACLMessage> orders = new ArrayDeque<>();
    private List<ProductModel> products;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.GREEN, jade.util.Logger.getMyLogger(this.getClass().getName()));


    @Override
    protected void setup() {
        logger.log(Level.INFO, "Warehouse is created");
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
            addBehaviour(new ReservationRequestBehaviour());
            addBehaviour(new ReservationBehaviour());
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
            }
        }
    }

    private class ReservationRequestBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("make-reservation"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                logger.log(Level.INFO, "Warehouse received request for reservation: " + msg.getContent());
                orders.add(msg);
            } else {
                block();
            }
        }
    }

    private class ReservationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            if (orders.isEmpty()) {
                return;
            }

            var visitorOrder = orders.remove();
            logger.log(Level.INFO, "Warehouse remove " + visitorOrder.getSender().getLocalName() + " from queue");
            var mapper = new ObjectMapper();
            List<ReservationModel> visitorDishList;
            try {
                visitorDishList = mapper.readValue(visitorOrder.getContent(), new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            for (var dish : visitorDishList) {
                HashMap<Integer, Double> dishProducts = dish.dishCardModel.getQuantityForProducts();
                dish.isReserved = true;
                for (var dishProduct : dishProducts.entrySet()) {
                    double cnt = 0;
                    for (var productModel : products) {
                        if (productModel.type == dishProduct.getKey()) {
                            cnt += productModel.quantity;
                        }
                    }
                    if (cnt >= dishProduct.getValue()) {
                        cnt = dishProduct.getValue();
                        for (var productModel : products) {
                            if (productModel.type == dishProduct.getKey()) {
                                if (cnt <= productModel.quantity) {
                                    productModel.quantity -= cnt;
                                    cnt = 0;
                                } else {
                                    cnt -= productModel.quantity;
                                    productModel.quantity = 0;
                                }
                            }
                        }
                        if (cnt != 0) {
                            dish.isReserved = false;
                        }
                    } else {
                        dish.isReserved = false;
                    }
                }
            }

            var reply = visitorOrder.createReply();
            try {
                reply.setContent(mapper.writeValueAsString(visitorDishList));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            reply.setPerformative(ACLMessage.INFORM);
            myAgent.send(reply);
            logger.log(Level.INFO, "Warehouse sent reply to order " + visitorOrder.getSender().getLocalName());
        }
    }

}
