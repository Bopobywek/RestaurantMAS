package ru.edu.hse;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import ru.edu.hse.agents.SupervisorAgent;

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

    void start() {
        try {
            containerController.createNewAgent("SupervisorAgent", SupervisorAgent.class.getName(), null).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}