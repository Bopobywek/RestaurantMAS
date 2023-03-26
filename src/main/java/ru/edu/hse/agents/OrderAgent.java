package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import jade.wrapper.AgentContainer;
import ru.edu.hse.models.*;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;
import ru.edu.hse.util.JsonMessage;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class OrderAgent extends Agent {
    private static AID visitor;
    private HashMap<Integer, DishCardModel> dishCardModels = new HashMap<>();
    private static final AID warehouse = new AID("WarehouseAgent", AID.ISLOCALNAME);

    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.BLUE, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    protected void setup() {
        logger.log(Level.INFO, MessageFormat.format("Order {0} is created.", getAID().getLocalName()));
        var args = getArguments();
        if (args != null) {
            var dishes = Arrays.stream((MenuDishModel[]) args[0]).toList();
            logger.log(Level.INFO, MessageFormat.format("OrderAgent received dishes of size {0}", dishes.size()));

            visitor = (AID) args[1];
            var dishCards = Arrays.stream((DishCardModel[]) args[2]).toList();

            for (var dishCard : dishCards) {
                dishCardModels.put(dishCard.id, dishCard);
            }
            for (var card : dishCardModels.entrySet()) {
                logger.log(Level.INFO, MessageFormat.format("OrderAgent: DishCard {0} loaded", card.getValue().name));
            }
            addBehaviour(new ValidateDishesBehaviour(visitor, dishes));
            // TODO: список продуктов из нужных диш кардов на каждое блюдо
        }

    }

    @Override
    protected void takeDown() {
        System.out.println(MessageFormat.format("Order {0} is terminated.", getAID().getLocalName()));
    }

    private class ValidateDishesBehaviour extends Behaviour {
        private static final String CONVERSATION_ID = "make-reservation";
        private List<MenuDishModel> finalMenuDishList;
        private List<MenuDishModel> dishes;
        private AID visitorAID;

        public ValidateDishesBehaviour(AID visitorAID, List<MenuDishModel> dishes) {
            this.visitorAID = visitorAID;
            this.dishes = dishes;
        }
        private int step = 0;
        public void action() {
            switch (step) {
                case 0 -> {
                    JsonMessage cfp = new JsonMessage(ACLMessage.REQUEST);
                    List<ReservationModel> reservationList = new ArrayList<>();
                    for(var dish : dishes) {
                        reservationList.add(new ReservationModel(dish, dishCardModels.get(dish.card)));
                    }

                    cfp.addReceiver(warehouse);

                    cfp.setContent(reservationList);
                    cfp.setConversationId(CONVERSATION_ID);

                    myAgent.send(cfp);

                    logger.log(Level.INFO, myAgent.getLocalName() + " send request to Warehouse for reservation");
                    step = 1;
                }
                case 1 -> {
                    var messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId(CONVERSATION_ID),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(messageTemplate);
                    List<ReservationModel> reservations;
                    if (msg != null) {
                        var mapper = new ObjectMapper();
                        try {
                            reservations = mapper.readValue(msg.getContent(), new TypeReference<>() {
                            });

                            logger.log(Level.INFO, myAgent.getLocalName() + " received reservations from warehouse");

                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        finalMenuDishList = new ArrayList<>();
                        for (var reservation : reservations) {
                            if (reservation.isReserved) {
                                finalMenuDishList.add(reservation.menuDishModel);
                            }
                        }
                        step = 2;

                    } else {
                        block();
                    }
                }
                case 2 -> {
                    // TODO: Создавать агентов процесса
                        logger.log(Level.INFO,
                                MessageFormat.format("For {0} final dish list size is {1}", visitorAID.getLocalName(), finalMenuDishList.size()));
                        int index = 0;
                        for (var dish : finalMenuDishList) {
                            createProcess(dishCardModels.get(dish.card), index);
                            ++index;
                        }
                        step = 3;
                }
            }
        }

        @Override
        public boolean done() {
            return step == 3;
        }

        private void createProcess(DishCardModel dishCard, int id) {
            AgentContainer container = getContainerController();
            try {
                container.createNewAgent(MessageFormat.format("ProcessAgent{0}$$${1}", id, myAgent.getLocalName()),
                        ProcessAgent.class.getName(), new Object[]{myAgent.getAID(), dishCard}).start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


}
