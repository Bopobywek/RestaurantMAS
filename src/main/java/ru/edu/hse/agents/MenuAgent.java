package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ru.edu.hse.models.DishCardModel;
import ru.edu.hse.models.MenuDishModel;
import ru.edu.hse.models.ProductModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class MenuAgent extends Agent {
    private List<MenuDishModel> menu;
    private final HashMap<Integer, DishCardModel> dishCardModels = new HashMap<>();
    private static final AID warehouse = new AID("WarehouseAgent", AID.ISLOCALNAME);
    private final ColorfulLogger logger =
            new ColorfulLogger(DebugColor.PURPLE, jade.util.Logger.getMyLogger(this.getClass().getName()));


    @Override
    protected void setup() {
        logger.log(Level.INFO, "Menu is created");
        Object[] args = getArguments();
        if (args != null) {
            menu = Arrays.stream((MenuDishModel[]) args[0]).toList();
            var dishCards = Arrays.stream((DishCardModel[]) args[1]).toList();
            for (var dishCard : dishCards) {
                dishCardModels.put(dishCard.id, dishCard);
            }
            for (var product : menu) {
                logger.log(Level.INFO, MessageFormat.format("{0} loaded", product.id));
            }
            for (var card : dishCardModels.entrySet()) {
                logger.log(Level.INFO, MessageFormat.format("{0} loaded", card.getValue().name));
            }
        }

        addBehaviour(new WarehouseStateRequestBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("Menu-agent " + getAID().getName() + " terminating.");
    }

    private class WarehouseStateRequestBehaviour extends CyclicBehaviour {
        static int step = 0;
        static ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("menu-get"),
                            MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        logger.log(Level.INFO, "Menu received request");
                        reply = msg.createReply();
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
                    cfp.addReceiver(warehouse);
                    cfp.setConversationId("warehouse-get");
                    logger.log(Level.INFO, "Menu send request to warehouse");
                    myAgent.send(cfp);
                    step = 2;
                }
                case 2 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("warehouse-get"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    if (msg != null) {
                        logger.log(Level.INFO, "Menu received available products: " + msg.getContent());

                        var mapper = new ObjectMapper();
                        List<ProductModel> products;
                        HashMap<Integer, Double> productModelHashMap = new HashMap<>();
                        try {
                            products = mapper.readValue(msg.getContent(),
                                    new TypeReference<>() {
                                    });
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        for (var product : products) {
                            if (productModelHashMap.containsKey(product.type)) {
                                double was = productModelHashMap.get(product.type);
                                productModelHashMap.put(product.type, was + product.quantity);
                            } else {
                                productModelHashMap.put(product.type, product.quantity);
                            }
                        }
                        updateMenu(productModelHashMap);
                        //
                        try {
                            reply.setContent(mapper.writeValueAsString(menu));
                            reply.setPerformative(ACLMessage.INFORM);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        logger.log(Level.INFO, "MenuAgent sent updated menu to Supervisor");
                        myAgent.send(reply);
                        step = 0;
                    } else {
                        block();
                    }
                }
            }
        }

        private void updateMenu(HashMap<Integer, Double> availableProducts) {
            for (var dish : menu) {
                var card = dishCardModels.get(dish.card);

                HashMap<Integer, Double> productsQuantity = card.getQuantityForProducts();

                for (var productEntry : productsQuantity.entrySet()) {
                    var productType = productEntry.getKey();
                    dish.isActive = availableProducts.get(productType) >= productEntry.getValue();
                }
            }
        }
    }

}
