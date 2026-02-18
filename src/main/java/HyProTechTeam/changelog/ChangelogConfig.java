package HyProTechTeam.changelog;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import java.util.Arrays;
import java.util.UUID;

public final class ChangelogConfig {
    private static final StringCodec STRING_CODEC = new StringCodec();
    private static final ArrayCodec<Entry> ENTRY_ARRAY_CODEC =
            ArrayCodec.ofBuilderCodec(Entry.CODEC, Entry[]::new);

    public static final class Entry {
        public static final BuilderCodec<Entry> CODEC =
                BuilderCodec.<Entry>builder(Entry.class, Entry::new)
                        .addField(new KeyedCodec<>("PlayerId", STRING_CODEC),
                                (entry, value) -> entry.playerId = normalize(value),
                                entry -> entry.playerId)
                        .addField(new KeyedCodec<>("LastSeen", STRING_CODEC),
                                (entry, value) -> entry.lastSeen = normalize(value),
                                entry -> entry.lastSeen)
                        .build();

        private String playerId;
        private String lastSeen;

        public Entry() {
            this("", "");
        }

        public Entry(String playerId, String lastSeen) {
            this.playerId = normalize(playerId);
            this.lastSeen = normalize(lastSeen);
        }

        public String getPlayerId() {
            return playerId;
        }

        public String getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(String lastSeen) {
            this.lastSeen = normalize(lastSeen);
        }
    }

    public static final BuilderCodec<ChangelogConfig> CODEC =
            BuilderCodec.<ChangelogConfig>builder(ChangelogConfig.class, ChangelogConfig::new)
                    .addField(new KeyedCodec<>("Players", ENTRY_ARRAY_CODEC),
                            (config, value) -> {
                                if (value != null) {
                                    config.players = value;
                                }
                            },
                            config -> config.players)
                    .build();

    private Entry[] players = new Entry[0];

    public String getLastSeen(UUID uuid) {
        if (uuid == null || players == null || players.length == 0) {
            return "";
        }
        String id = uuid.toString();
        for (Entry entry : players) {
            if (entry != null && id.equals(entry.playerId)) {
                return entry.lastSeen == null ? "" : entry.lastSeen;
            }
        }
        return "";
    }

    public void markSeen(UUID uuid, String version) {
        if (uuid == null) {
            return;
        }
        String id = uuid.toString();
        if (players == null) {
            players = new Entry[0];
        }
        for (Entry entry : players) {
            if (entry != null && id.equals(entry.playerId)) {
                entry.setLastSeen(version);
                return;
            }
        }
        Entry[] expanded = Arrays.copyOf(players, players.length + 1);
        expanded[players.length] = new Entry(id, version);
        players = expanded;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
