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
import jade.wrapper.AgentContainer;
import ru.edu.hse.configuration.JadeAgent;
import ru.edu.hse.models.*;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

@JadeAgent
public class SupervisorAgent extends Agent {
    private static List<MenuDishModel> menuItems;
    private static final Queue<ACLMessage> visitors = new ArrayDeque<>();
    private static final AID menu = new AID("MenuAgent", AID.ISLOCALNAME);
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.YELLOW, jade.util.Logger.getMyLogger(this.getClass().getName()));

    private AgentContainer container;


    @Override
    protected void setup() {
        logger.log(Level.INFO, "Supervisor is created");
        container = getContainerController();
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

        createMenu();
        createWarehouse();
        createEquipment();
        createVisitors();

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

                        var visitorOrder = visitors.remove();

                        List<DishModel> visitorDishList;
                        try {
                            visitorDishList = mapper.readValue(visitorOrder.getContent(), new TypeReference<>() {
                            });
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        // TODO: проверить по меню
                        var validDishes = new HashMap<Integer, MenuDishModel>();
                        for (var validDish : menuItems) {
                            validDishes.put(validDish.id, validDish);
                        }

                        List<MenuDishModel> orderDishes = new ArrayList<>();
                        for (var dish : visitorDishList) {
                            if (validDishes.containsKey(dish.menuId) && validDishes.get(dish.menuId).isActive) {
                                orderDishes.add(validDishes.get(dish.menuId));
                            }
                        }

                        // TODO: создать заказ
                        createOrderAgent(visitorOrder.getSender(), orderDishes);
                        logger.log(Level.INFO,
                                "Supervisor creates order for " + visitorOrder.getSender().getLocalName());
                        step = 0;
                    } else {
                        block();
                    }
                }
            }
        }

        private void createOrderAgent(AID aid, List<MenuDishModel> order) {
            try {
                var mapper = new ObjectMapper();
                DishCardsModel dishCardsModel = mapper.readValue(getClass().getClassLoader().getResource("dish_cards.json"),
                        DishCardsModel.class);
                MenuDishModel[] orderArray = new MenuDishModel[order.size()];
                for (int i = 0; i < order.size(); ++i) {
                    orderArray[i] = order.get(i);
                }
                container.createNewAgent(MessageFormat.format("OrderAgent$$${0}", aid.getLocalName()),
                        OrderAgent.class.getName(), new Object[]{orderArray, aid, dishCardsModel.dishCardModels}).start();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    void createMenu() {
        var mapper = new ObjectMapper();
        MenuDishesModel model;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("menu_dishes.json"),
                            MenuDishesModel.class);
            DishCardsModel dishCardsModel = mapper.readValue(getClass().getClassLoader().getResource("dish_cards.json"),
                    DishCardsModel.class);
            container.createNewAgent("MenuAgent", MenuAgent.class.getName(),
                    new Object[]{model.menuDishModels, dishCardsModel.dishCardModels}).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    void createWarehouse() {
        var mapper = new ObjectMapper();
        ProductsModel model = null;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("products.json"),
                            ProductsModel.class);
            container.createNewAgent("WarehouseAgent", WarehouseAgent.class.getName(), model.productsModels()).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createVisitors() {
        try {
            var mapper = new ObjectMapper();
            var models = mapper.
                    readValue(getClass().getClassLoader().getResource("visitors_orders.json"),
                            VisitorsOrdersModel.class);
            int index = 0;
            for (var visitorModel : models.visitorModels()) {
                container.createNewAgent(visitorModel.name, VisitorAgent.class.getName(),
                        new Object[]{visitorModel}).start();
                ++index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createEquipment() {
        var mapper = new ObjectMapper();
        EquipmentCollectionModel model = null;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("equipment.json"),
                            EquipmentCollectionModel.class);
            int index = 0;
            for (var equipModel : model.equipmentModels()) {
                container.createNewAgent(MessageFormat.format("{0}{1}", EquipmentAgent.class.getName(), index), EquipmentAgent.class.getName(),
                        new Object[]{equipModel}).start();
                ++index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
