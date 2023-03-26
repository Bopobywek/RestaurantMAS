package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import ru.edu.hse.configuration.JadeAgent;
import ru.edu.hse.models.*;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

@JadeAgent
public class SupervisorAgent extends Agent {
    private static List<MenuDishModel> menuItems;
    private static final Queue<ACLMessage> visitors = new ArrayDeque<>();
    private static final Queue<ACLMessage> operations = new ArrayDeque<>();
    private static final AID menu = new AID("MenuAgent", AID.ISLOCALNAME);
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.BLOOD_COLOR, jade.util.Logger.getMyLogger(this.getClass().getName()));

    private List<AID> cooks = new ArrayList<>();
    private List<AID> equipments = new ArrayList<>();
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
        createCookers();
        doWait(1000);

        DFAgentDescription template = new DFAgentDescription();
        serviceDescription = new ServiceDescription();
        serviceDescription.setType("cook-service");
        template.addServices(serviceDescription);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (var cook : result) {
                cooks.add(cook.getName());
                //logger.log(Level.INFO, MessageFormat.format("Found cook: {0}.", cook.getName()));
            }

            serviceDescription.setType("equipment-service");
            template.addServices(serviceDescription);
            result = DFService.search(this, template);
            for (var equipment : result) {
                equipments.add(equipment.getName());
                //logger.log(Level.INFO, MessageFormat.format("Found equipment: {0}.", equipment.getName()));
            }
        } catch (FIPAException e) {
            throw new RuntimeException(e);
        }

        createVisitors();

        addBehaviour(new ReceiveOrderBehaviour());
        addBehaviour(new OrderServerBehaviour());
        addBehaviour(new ReceiveOperationBehaviour());
        addBehaviour(new OperationServerBehaviour());
    }

    private void saveLogs() {
        var operationsLog = new OperationsLogModel();
        operationsLog.log = OperationAgent.logModelQueue.stream().sorted(Comparator.comparingInt(x -> x.id)).toList();

        var processesLogs = new ProcessesLogModel();
        processesLogs.processesLog = ProcessAgent.logModelQueue.stream().sorted(Comparator.comparingInt(x -> x.id)).toList();

        var mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        try {
            writer.writeValue(new File("output/operation_log.json"), operationsLog);
            writer.writeValue(new File("output/process_log.json"), processesLogs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            saveLogs();
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

    private class ReceiveOperationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("operation-reservation"),
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
            ACLMessage msg = myAgent.receive(messageTemplate);
            if (msg != null) {
                logger.log(Level.INFO, "Supervisor received operation: " + msg.getContent());
                operations.add(msg);
            } else {
                block();
            }
        }
    }

    private class OperationServerBehaviour extends CyclicBehaviour {
        private int step = 0;
        private int cookIndex = 0;
        private int equipmentIndex = 0;
        private OperationModel operation;
        private String operationName;
        private AID cook;
        private AID equipment;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    cook = null;
                    equipment = null;
                    if (!operations.isEmpty()) {
                        ACLMessage operationMessage = operations.poll();
                        if (operationMessage == null) {
                            return;
                        }
                        var mapper = new ObjectMapper();
                        try {
                            operationName = operationMessage.getSender().getLocalName();
                            operation = mapper.readValue(operationMessage.getContent(), OperationModel.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        step = 1;
                    }
                }
                case 1 -> {
                    if (equipment == null) {
                        JsonMessage equipmentMsg = new JsonMessage(ACLMessage.REQUEST);
                        equipmentMsg.setConversationId("equipment-reservation");
                        equipmentMsg.setContent(operation.equipmentType);
                        if (equipmentIndex >= equipments.size()) {
                            equipmentIndex = 0;
                        }
                        equipmentMsg.addReceiver(equipments.get(equipmentIndex));
                        myAgent.send(equipmentMsg);
                        step = 2;
                    } else if (cook == null) {
                        ACLMessage cookMsg = new ACLMessage(ACLMessage.REQUEST);
                        cookMsg.setConversationId("cook-reservation");
                        if (cookIndex >= cooks.size()) {
                            cookIndex = 0;
                        }
                        cookMsg.addReceiver(cooks.get(cookIndex));
                        myAgent.send(cookMsg);
                        step = 2;
                    } else {
                        step = 3;
                    }
                }
                case 2 -> {
                    if (equipment == null) {
                        var messageTemplateEquipment = MessageTemplate.and(MessageTemplate.MatchConversationId("equipment-reservation"),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                        ACLMessage msgEquipment = myAgent.receive(messageTemplateEquipment);
                        boolean response = false;
                        if (msgEquipment != null) {
                            var mapper = new ObjectMapper();
                            try {
                                response = mapper.readValue(msgEquipment.getContent(), boolean.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (!response) {
                            ++equipmentIndex;
                        } else {
                            equipment = msgEquipment.getSender();
                        }
                        // т.к. не всех нашли
                        step = 1;
                    } else if (cook == null) {
                        var messageTemplateCook = MessageTemplate.and(MessageTemplate.MatchConversationId("cook-reservation"),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                        ACLMessage msgCook = myAgent.receive(messageTemplateCook);
                        boolean response = false;
                        if (msgCook != null) {
                            var mapper = new ObjectMapper();
                            try {
                                response = mapper.readValue(msgCook.getContent(), boolean.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        if (!response) {
                            ++cookIndex;
                            step = 1;
                        } else {
                            cook = msgCook.getSender();
                            step = 3;
                        }
                        // т.к. не проверили ещё equip

                    }
                }
                case 3 -> {
                    // Нужно отправить операции ответное сообщение со стороны поваров и оборудования,
                    // когда они закончат её выполнять.
                    var operationExecution = new OperationExecutionModel();
                    operationExecution.operationName = operationName;
                    operationExecution.time = operation.time;

                    JsonMessage cookRequestMessage = new JsonMessage(ACLMessage.REQUEST);
                    cookRequestMessage.addReceiver(cook);
                    cookRequestMessage.setContent(operationExecution);

                    cookRequestMessage.setConversationId("cook-start");
                    myAgent.send(cookRequestMessage);

                    JsonMessage equipRequestMessage = new JsonMessage(ACLMessage.REQUEST);
                    equipRequestMessage.addReceiver(equipment);
                    equipRequestMessage.setContent(operationExecution);
                    equipRequestMessage.setConversationId("equipment-start");
                    myAgent.send(equipRequestMessage);

                    // Начинаем искаьб

                    step = 0;
                }
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

            for (var equipModel : model.equipmentModels()) {
                container.createNewAgent(MessageFormat.format("EquipmentAgent{0}", equipModel.id), EquipmentAgent.class.getName(),
                        new Object[]{equipModel}).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createCookers() {
        var mapper = new ObjectMapper();
        CookersModel model = null;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("cookers.json"),
                            CookersModel.class);

            for (var cook : model.cookers) {
                container.createNewAgent(MessageFormat.format("CookAgent{0}", cook.id), CookAgent.class.getName(),
                        new Object[]{cook}).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
