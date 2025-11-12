package net.errantchaosie.tradeandopenmarkets.shop;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.errantchaosie.tradeandopenmarkets.TradeAndOpenMarketsMod;
import net.errantchaosie.tradeandopenmarkets.shop.tab.config.ShopTabDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Loads shop definitions from data packs. Each json file in data/<namespace>/shops defines
 * the tab layout for a single profession.
 */
public class ShopDefinitionLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "shops";
    private static final ShopDefinitionLoader INSTANCE = new ShopDefinitionLoader();

    private volatile Map<ResourceLocation, ShopDefinition> definitions = Map.of();

    private ShopDefinitionLoader() {
        super(GSON, FOLDER);
    }

    public static ShopDefinitionLoader getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ShopDefinition> parsed = new HashMap<>();
        object.forEach((fileId, jsonElement) -> {
            try {
                JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "shop");
                ShopDefinition definition = parseDefinition(fileId, jsonObject);
                parsed.put(definition.profession(), definition);
            } catch (RuntimeException exception) {
                TradeAndOpenMarketsMod.LOGGER.error("Failed to parse shop definition {}", fileId, exception);
            }
        });
        this.definitions = Map.copyOf(parsed);
        ShopTabManager.INSTANCE.invalidate();
    }

    private ShopDefinition parseDefinition(ResourceLocation fileId, JsonObject jsonObject) {
        ResourceLocation profession = resolveProfession(fileId, jsonObject);
        List<ShopTabDefinition> tabs = new ArrayList<>();
        JsonArray tabArray = jsonObject.has("tabs") ? GsonHelper.getAsJsonArray(jsonObject, "tabs") : new JsonArray();
        for (JsonElement element : tabArray) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("Expected tab object but found " + element);
            }
            tabs.add(parseTab(element.getAsJsonObject()));
        }
        return new ShopDefinition(profession, tabs);
    }

    private ResourceLocation resolveProfession(ResourceLocation fileId, JsonObject jsonObject) {
        if (jsonObject.has("profession")) {
            return ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "profession"));
        }
        return fileId;
    }

    private ShopTabDefinition parseTab(JsonObject tabObject) {
        ResourceLocation type = ResourceLocation.parse(GsonHelper.getAsString(tabObject, "type"));
        Component title = parseTitle(type, tabObject.get("title"));
        JsonObject data = tabObject.has("data") ? GsonHelper.getAsJsonObject(tabObject, "data") : new JsonObject();
        return new ShopTabDefinition(type, title, data);
    }

    private Component parseTitle(ResourceLocation type, @Nullable JsonElement element) {
        if (element == null) {
            return defaultTitle(type);
        }
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(message -> TradeAndOpenMarketsMod.LOGGER.warn("Failed to parse shop tab title: {}", message))
                .orElseGet(() -> defaultTitle(type));
    }

    private Component defaultTitle(ResourceLocation type) {
        String path = type.getPath();
        if (path.isEmpty()) {
            return Component.empty();
        }
        String pretty = Character.toUpperCase(path.charAt(0)) + path.substring(1).replace('_', ' ');
        return Component.literal(pretty);
    }

    public Optional<ShopDefinition> getDefinition(ResourceLocation profession) {
        return Optional.ofNullable(this.definitions.get(profession));
    }

    public Collection<ShopDefinition> getAllDefinitions() {
        return Collections.unmodifiableCollection(this.definitions.values());
    }
}
