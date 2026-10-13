package com.xyzmod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Per-player story progress, saved with the world. */
public class XyzData extends SavedData {

    public static final class Pending {
        public int delay;
        public final Consumer<ServerPlayer> action;
        public Pending(int delay, Consumer<ServerPlayer> action) { this.delay = delay; this.action = action; }
    }

    public static final class PlayerState {
        // ---- saved ----
        public long ticks;        // play time in ticks
        public int stage;         // 0 wrongness, 1 presence, 2 stalking, 3 hunt
        public int announced;     // highest stage whose transition event already ran
        public int bedSets;       // how many times the player set a spawn on a bed
        public int deaths;
        public int sessions;
        public int storyIndex;    // how far along the ordered story lines we are

        // ---- runtime only ----
        public int eventCooldown = 60;   // seconds
        public int appearCooldown = 30;  // seconds
        public int anger;                // rises when the player hits him, decays 1/s
        public boolean killedByXyz;
        public final List<Pending> pending = new ArrayList<>();

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("ticks", ticks);
            t.putInt("stage", stage);
            t.putInt("announced", announced);
            t.putInt("bedSets", bedSets);
            t.putInt("deaths", deaths);
            t.putInt("sessions", sessions);
            t.putInt("storyIndex", storyIndex);
            return t;
        }

        static PlayerState load(CompoundTag t) {
            PlayerState s = new PlayerState();
            s.ticks = t.getLong("ticks");
            s.stage = t.getInt("stage");
            s.announced = t.getInt("announced");
            s.bedSets = t.getInt("bedSets");
            s.deaths = t.getInt("deaths");
            s.sessions = t.getInt("sessions");
            s.storyIndex = t.getInt("storyIndex");
            return s;
        }
    }

    private final Map<UUID, PlayerState> players = new HashMap<>();

    public static XyzData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(XyzData::load, XyzData::new, "xyzmod");
    }

    public PlayerState of(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerState());
    }

    public void reset(UUID id) {
        players.remove(id);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, PlayerState> e : players.entrySet()) {
            CompoundTag c = e.getValue().save();
            c.putUUID("id", e.getKey());
            list.add(c);
        }
        tag.put("players", list);
        return tag;
    }

    public static XyzData load(CompoundTag tag) {
        XyzData d = new XyzData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            d.players.put(c.getUUID("id"), PlayerState.load(c));
        }
        return d;
    }
}
