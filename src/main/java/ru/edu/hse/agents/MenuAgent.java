package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.util.Logger;
import ru.edu.hse.models.MenuDishModel;
import ru.edu.hse.models.MenuDishesModel;
import ru.edu.hse.models.ProductModel;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

public class MenuAgent extends Agent {
    private List<MenuDishModel> dishes;
    private final Logger logger = jade.util.Logger.getMyLogger(this.getClass().getName());

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            dishes = Arrays.stream((MenuDishModel[]) args).toList();
            for (var product : dishes) {
                System.out.println(MessageFormat.format("{0} loaded", product.id));
            }
        }

        var mapper = new ObjectMapper();
        for (var product : dishes) {
            try {
                System.out.println(mapper.writeValueAsString(product));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Menu-agent " + getAID().getName() + " terminating.");
    }
}
