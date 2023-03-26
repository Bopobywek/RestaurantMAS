package ru.edu.hse.agents;

import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import ru.edu.hse.models.EquipmentModel;
import ru.edu.hse.util.ColorfulLogger;
import ru.edu.hse.util.DebugColor;

import java.text.MessageFormat;
import java.util.logging.Level;

public class EquipmentAgent extends Agent {
    private String name;
    private final ColorfulLogger logger = new ColorfulLogger(DebugColor.SAND, jade.util.Logger.getMyLogger(this.getClass().getName()));

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null) {
            var model = (EquipmentModel) getArguments()[0];
            name = model.name;
        }
        logger.log(Level.INFO, MessageFormat.format("{0} equipment loaded", name));
        addBehaviour(new WakerBehaviour(this, 6000) {
            @Override
            protected void onWake() {
                doDelete();
            }
        });
    }
}
