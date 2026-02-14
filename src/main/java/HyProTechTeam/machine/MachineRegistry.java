package HyProTechTeam.machine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MachineRegistry {
    private static final Map<String, MachineDefinition> BY_ID = new ConcurrentHashMap<>();
    private static final List<MachineDefinition> DEFINITIONS = new CopyOnWriteArrayList<>();

    private MachineRegistry() {
    }

    public static void register(MachineDefinition definition) {
        if (definition == null || definition.id() == null) {
            return;
        }
        BY_ID.put(definition.id(), definition);
        if (!DEFINITIONS.contains(definition)) {
            DEFINITIONS.add(definition);
        }
    }

    public static MachineDefinition getById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return BY_ID.get(id);
    }

    public static MachineDefinition findByBlockId(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }
        for (MachineDefinition definition : DEFINITIONS) {
            if (definition.matchesBlockId(blockId)) {
                return definition;
            }
        }
        return null;
    }

    public static List<MachineDefinition> all() {
        return Collections.unmodifiableList(DEFINITIONS);
    }
}
