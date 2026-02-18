package HyProTechTeam.machine;

import HyProTechTeam.energy.EnergyNodeComponent;
import HyProTechTeam.energy.EnergySide;

public abstract class MasterMachine implements MachineDefinition {
    protected int defaultCapacity() {
        return 50000;
    }

    protected int defaultMaxTransfer() {
        return 5000;
    }

    protected int defaultProgressMax() {
        return 100;
    }

    @Override
    public void configureDefaults(MachineComponent machine, EnergyNodeComponent energy) {
        if (energy != null) {
            if (energy.getNodeType() != EnergyNodeComponent.NodeType.MACHINE) {
                energy.setNodeType(EnergyNodeComponent.NodeType.MACHINE);
            }
            if (energy.getCapacity() <= 0) {
                energy.setCapacity(defaultCapacity());
            }
            if (energy.getMaxTransfer() <= 0) {
                energy.setMaxTransfer(defaultMaxTransfer());
            }
            if (energy.getInputMask() == 0 && energy.getOutputMask() == 0) {
                energy.setInputMask(EnergySide.ALL_MASK);
                energy.setOutputMask(EnergySide.ALL_MASK);
            }
        }
        if (machine != null && machine.getProgressMax() <= 0) {
            machine.setProgressMax(defaultProgressMax());
        }
    }
}
