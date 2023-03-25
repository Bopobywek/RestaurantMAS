package ru.edu.hse;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.reflections.Reflections;
import ru.edu.hse.agents.EquipmentAgent;
import ru.edu.hse.agents.MenuAgent;
import ru.edu.hse.agents.VisitorAgent;
import ru.edu.hse.agents.WarehouseAgent;
import ru.edu.hse.configuration.JadeAgent;
import ru.edu.hse.models.EquipmentCollectionModel;
import ru.edu.hse.models.MenuDishesModel;
import ru.edu.hse.models.ProductsModel;
import ru.edu.hse.models.VisitorsOrdersModel;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;

class MainController {

    private final ContainerController containerController;

    public MainController() {
        final Runtime rt = Runtime.instance();
        final Profile p = new ProfileImpl();

        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "8080");
        p.setParameter(Profile.GUI, "true");

        containerController = rt.createMainContainer(p);
    }

    void initAgents() {
        var basePackage = MainController.class.getPackageName();
        initAgents(basePackage);
        createVisitors();
        createWarehouse();
        createEquipment();
        createMenu();
    }

    void createMenu() {
        var mapper = new ObjectMapper();
        MenuDishesModel model;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("menu_dishes.json"),
                            MenuDishesModel.class);
            createAgent(MenuAgent.class, MenuAgent.class.getName(),
                    model.menuDishModels).start();
        } catch (Exception ex) {
            ex.printStackTrace();
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
                createAgent(EquipmentAgent.class, MessageFormat.format("{0}{1}", EquipmentAgent.class.getName(), index),
                        new Object[]{equipModel}).start();
                ++index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void createWarehouse() {
        var mapper = new ObjectMapper();
        ProductsModel model = null;
        try {
            model = mapper.
                    readValue(getClass().getClassLoader().getResource("products.json"),
                            ProductsModel.class);
            createAgent(WarehouseAgent.class, WarehouseAgent.class.getName(), model.productsModels()).start();
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
                createAgent(VisitorAgent.class, visitorModel.name,
                        new Object[]{visitorModel}).start();
                ++index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void initAgents(String basePackage) {
        final Reflections reflections = new Reflections(basePackage);

        final Set<Class<?>> allClasses = reflections.getTypesAnnotatedWith(JadeAgent.class);
        try {
            for (Class<?> clazz : allClasses) {
                if (!clazz.equals(VisitorAgent.class) && Agent.class.isAssignableFrom(clazz)) {
                    configureAgent(clazz);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureAgent(Class<?> clazz) throws StaleProxyException {
        final JadeAgent jadeAgent = clazz.getAnnotation(JadeAgent.class);

        if (jadeAgent.number() <= 0) {
            throw new IllegalStateException(MessageFormat.format(
                    "Number of agent {0} is less then 1. Real number is {1}",
                    clazz.getName(),
                    jadeAgent.number()
            ));
        }

        final String agentName =
                !Objects.equals(jadeAgent.value(), "")
                        ? jadeAgent.value()
                        : clazz.getSimpleName();

        if (jadeAgent.number() == 1) {
            createAgent(clazz, agentName).start();
        } else {
            for (int i = 0; i < jadeAgent.number(); ++i) {
                createAgent(
                        clazz,
                        MessageFormat.format(
                                "{0}{1}",
                                agentName,
                                i
                        )).start();
            }
        }
    }

    private AgentController createAgent(Class<?> clazz, String agentName) throws StaleProxyException {
        return containerController.createNewAgent(
                agentName,
                clazz.getName(),
                null);
    }

    private AgentController createAgent(Class<?> clazz, String agentName, Object[] args) throws StaleProxyException {
        return containerController.createNewAgent(
                agentName,
                clazz.getName(),
                args);
    }
}
