package com.errantmc.markets.npcinteractions.registry;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.errantmc.markets.ErrantTrade;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.jline.utils.Log;
import org.slf4j.Logger;

import java.util.*;

/**
 * Loads npcinteractions definitions from data packs. Each json file in data/<namespace>/npcinteractions defines
 * the tabs layout for a single profession.
 */
public class InteractionDefinitionLoader extends SimpleJsonResourceReloadListener {
    public static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "npcinteractions";
    private static final InteractionDefinitionLoader INSTANCE = new InteractionDefinitionLoader();

    private volatile Map<ResourceLocation, InteractionDefinition> definitions = Map.of();

    private InteractionDefinitionLoader() {
        super(GSON, FOLDER);
    }

    public static InteractionDefinitionLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - apply");
        Map<ResourceLocation, InteractionDefinition> parsed = new HashMap<>();
        object.forEach((fileId, jsonElement) -> {
            try {

                JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "npcinteractions");
                InteractionDefinition definition = parseDefinition(fileId, jsonObject);
                parsed.put(definition.profession(), definition);
                LOG.debug(String.format("InteractionDefinitionLoader - Loaded Interaction Definition %s", definition.profession()));
            } catch (RuntimeException exception) {

                LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - apply - exception caught");
                LOG.debug(exception.getMessage());
                ErrantTrade.LOGGER.error("Failed to parse npcinteractions definition {}", fileId, exception);
            }
        });
        this.definitions = Map.copyOf(parsed);
        InteractionTabManager.INSTANCE.invalidate();
    }

    private InteractionDefinition parseDefinition(ResourceLocation fileId, JsonObject jsonObject) {
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - parseDefinition");
        ResourceLocation profession = resolveProfession(fileId, jsonObject);
        List<InteractionTabConfig> tabs = new ArrayList<>();
        JsonArray tabArray = jsonObject.has("tabs") ? GsonHelper.getAsJsonArray(jsonObject, "tabs") : new JsonArray();
        for (JsonElement element : tabArray) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("Expected tabs object but found " + element);
            }
            tabs.add(parseTab(element.getAsJsonObject()));
        }
        return new InteractionDefinition(profession, tabs);
    }

    private ResourceLocation resolveProfession(ResourceLocation fileId, JsonObject jsonObject) {
        if (jsonObject.has("profession")) {
            var professionStr = GsonHelper.getAsString(jsonObject, "profession");
            LOG.debug("InteractionDefinitionLoader - Loading Interaction Definition - profession {}", professionStr);
            return ResourceLocation.parse(professionStr);
        }
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - resolveProfession - defaulting to fileId");
        return fileId;
    }

    private InteractionTabConfig parseTab(JsonObject tabObject) {
        ResourceLocation type = ResourceLocation.parse(GsonHelper.getAsString(tabObject, "type"));
        Component title = parseTitle(type, tabObject.get("title"));
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definition - title {}", title);
        JsonObject data = tabObject.has("data") ? GsonHelper.getAsJsonObject(tabObject, "data") : new JsonObject();
        return new InteractionTabConfig(type, title, data);
    }

    private Component parseTitle(ResourceLocation type, @Nullable JsonElement element) {
        if (element == null) {
            LOG.debug("InteractionDefinitionLoader - Loading Interaction Definition - parseTitle - default title");
            return defaultTitle(type);
        }
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(message -> ErrantTrade.LOGGER.warn("Failed to parse npcinteractions tabs title: {}", message))
                .orElseGet(() -> defaultTitle(type));
    }

    private Component defaultTitle(ResourceLocation type) {
        String path = type.getPath();
        if (path.isEmpty()) {
            LOG.debug("InteractionDefinitionLoader - Loading Interaction Definition - defaultTitle path is empty");
            return Component.empty();
        }
        String pretty = Character.toUpperCase(path.charAt(0)) + path.substring(1).replace('_', ' ');
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definition - defaultTitle pretty {}", pretty);
        return Component.literal(pretty);
    }

    public Optional<InteractionDefinition> getDefinition(ResourceLocation profession) {
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - getDefinition for profession {}", profession);
        return Optional.ofNullable(this.definitions.get(profession));
    }

    public Collection<InteractionDefinition> getAllDefinitions() {
        var size = this.definitions.size();
        LOG.debug("InteractionDefinitionLoader - Loading Interaction Definitions - getAllDefinitions size {}", size);
        for (var entry : this.definitions.entrySet()) {
            LOG.debug("InteractionDefinitionLoader - Definition for profession {}: {}", entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableCollection(this.definitions.values());
    }
}
