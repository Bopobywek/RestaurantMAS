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
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.PURPLE, jade.util.Logger.getMyLogger(this.getClass().getName()));


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
        // Printout a dismissal message
        System.out.println("Menu-agent " + getAID().getName() + " terminating.");
    }

//    private class SendMenuBehaviour extends CyclicBehaviour {
//        @Override
//        public void action() {
//            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("menu-get"),
//                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
//            ACLMessage msg = myAgent.receive(messageTemplate);
//            if (msg != null) {
//                logger.log(Level.INFO, "Menu received request");
//                addBehaviour(new WarehouseStateRequestBehaviour());
//            } else {
//                block();
//            }
//        }
//    }

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

        /*
        "availableProducts": [
    {
      "prod_item_id": 172,
      "prod_item_type": 18,
      "prod_item_name": "Princess Nuri tea in bags",
      "prod_item_company": "ORIMI",
      "prod_item_unit": "pc.",
      "prod_item_quantity": 874,
      "prod_item_cost": 1.5,
      "prod_item_delivered": "2023-01-15T08:10:36",
      "prod_item_valid_until": "2024-12-31T23:59:59"
    }
    ]

    "dish_cards": [
    {
      "card_id": 518,
      "dish_name": "Princess Nuri tea bag in a paper cup",
      "card_descr": "pouring boiled water into a paper cup + 2 bags of sugar",
      "card_time": 0.15,
      "equip_type": 25,
      "operations": [
        {
          "oper_type": 17,
          "oper_time": 0.15,
          "oper_async_point": 0,
          "oper_products": [
            {
              "prod_type": 18,
              "prod_quantity": 1
            }
            ]
          }
        ]
       }
         */
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
