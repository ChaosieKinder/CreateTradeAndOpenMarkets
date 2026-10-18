package net.errantchaosie.tradeandopenmarkets.npcinteractions.registry;

import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Parsed configuration describing a tabs entry within a npcinteractions definition.
 */
public class InteractionTabConfig {

    private final ResourceLocation type;
    private final Component title;
    private final JsonObject rawData;

    public InteractionTabConfig(ResourceLocation type, Component title, JsonObject data) {
        this.type = type;
        this.title = title;
        this.rawData = data;
        parseData();
    }

    public void parseData() {
        // Implementation for parsing the data JsonObject
    }

    public ResourceLocation getType() {
        return type;
    }

    public Component getTitle() {
        return title;
    }

    public JsonObject getRawData() {
        return rawData;
    }
}
