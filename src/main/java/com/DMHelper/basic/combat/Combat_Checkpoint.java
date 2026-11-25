package com.DMHelper.basic.combat;

import com.google.gson.*;
import java.time.LocalDateTime;
import java.util.List;

/** Versioned encounter data; object references are restored through combatant indexes. */
public final class Combat_Checkpoint {
    private Combat_Checkpoint() {}
    public record Saved(int version, String id, boolean started, List<String> logs,
                        Combat_Engine.SavedState engine, Battle_Grid.SavedState grid) {}
    private static final Gson GSON = new GsonBuilder().serializeNulls()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                public boolean shouldSkipField(FieldAttributes field) {
                    return field.getDeclaringClass()==Combatant.class
                            && (field.getName().equals("linked_character") || field.getName().equals("linked_monster"));
                }
                public boolean shouldSkipClass(Class<?> type) { return false; }
            })
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (value,type,context) -> new JsonPrimitive(value.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (value,type,context) -> LocalDateTime.parse(value.getAsString()))
            .create();
    public static String encode(Saved saved) { return GSON.toJson(saved); }
    public static Saved decode(String json) {
        Saved saved=GSON.fromJson(json,Saved.class);
        if(saved==null || saved.version!=1 || saved.id==null || saved.logs==null || saved.engine==null || saved.grid==null)
            throw new IllegalArgumentException("战斗存档损坏或版本不兼容");
        return saved;
    }
}
