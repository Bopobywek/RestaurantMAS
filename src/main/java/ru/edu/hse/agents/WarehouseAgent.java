package ru.edu.hse.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.util.Logger;
import ru.edu.hse.models.ProductModel;
import ru.edu.hse.models.VisitorModel;

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
                    System.out.println(mapper.writeValueAsString(product));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Warehouse-agent " + getAID().getName() + " terminating.");
    }
}
