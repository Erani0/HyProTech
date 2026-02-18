package HyProTechTeam.machine;

import HyProTechTeam.energy.EnergyNodeComponent;
import HyProTechTeam.energy.EnergySide;

public interface MachineDefinition {
    String id();

    boolean matchesBlockId(String blockId);

    default MachineComponent createMachineComponent() {
        MachineComponent component = new MachineComponent();
        component.setMachineId(id());
        return component;
    }

    default EnergyNodeComponent createEnergyNode() {
        EnergyNodeComponent node = new EnergyNodeComponent();
        node.setNodeType(EnergyNodeComponent.NodeType.MACHINE);
        node.setInputMask(EnergySide.ALL_MASK);
        node.setOutputMask(EnergySide.ALL_MASK);
        return node;
    }

    default void configureDefaults(MachineComponent machine, EnergyNodeComponent energy) {
    }

    /**
     * @return true if the machine component changed and should be saved.
     */
    boolean tick(MachineContext context, float deltaSeconds);
}
