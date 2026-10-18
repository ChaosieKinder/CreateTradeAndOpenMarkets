package com.errantmc.markets.gametest;

import com.errantmc.markets.ErrantTrade;
import com.errantmc.markets.block.TradeBlocks;
import com.errantmc.markets.data.CoinDenomination;
import com.errantmc.markets.item.CoinItem;
import com.errantmc.markets.item.TradeItems;
import com.errantmc.markets.registry.TradeCreativeModeTabs;
import com.errantmc.markets.villager.TradeVillagers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.neoforged.fml.ModList;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.errantmc.markets.gametest.GameTests.*;

/**
 * Lists that must agree with a registry. Each drifts silently: a missing name renders as its raw
 * key, a missing model as the purple-black checkerboard, an unlisted item is simply absent.
 */
@GameTestHolder(ErrantTrade.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {
    private ContentGameTests() {}

    /** Kept out of the creative tab on purpose: it only comes from breaking a big pile (README). */
    private static Set<Item> notInCreativeTab() {
        return Set.of(TradeItems.COIN_STACK_ITEM.get());
    }

    /**
     * Every crafting recipe made only of coins conserves value, counted in copper bits. A recipe
     * that does not is a mint (or a sink) sitting in the crafting grid.
     */
    @GameTest(template = ARENA)
    public static void everyCoinRecipeConservesValue(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        int checked = 0;
        List<String> wrong = new ArrayList<>();
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(registries);
            if (!(result.getItem() instanceof CoinItem out)) continue;
            long in = 0;
            boolean allCoins = true;
            for (Ingredient ingredient : holder.value().getIngredients()) {
                ItemStack[] options = ingredient.getItems();
                if (options.length != 1 || !(options[0].getItem() instanceof CoinItem coin)) {
                    allCoins = false;
                    break;
                }
                in += (long) coin.denom().value * options[0].getCount();
            }
            if (!allCoins) continue;
            checked++;
            long outValue = (long) out.denom().value * result.getCount();
            if (in != outValue) wrong.add(holder.id() + ": " + in + " in, " + outValue + " out");
        }
        if (!wrong.isEmpty()) helper.fail("value not conserved by " + wrong);
        assertTrue(helper, "at least the 18 coin recipes to be checked, found " + checked, checked >= 18);
        helper.succeed();
    }

    /** Every denomination can be crafted down into the next smaller one and up into the next larger. */
    @GameTest(template = ARENA)
    public static void everyDenominationConvertsBothWays(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        Map<CoinDenomination, Set<CoinDenomination>> madeFrom = new EnumMap<>(CoinDenomination.class);
        for (var holder : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (!(holder.value().getResultItem(registries).getItem() instanceof CoinItem out)) continue;
            for (Ingredient ingredient : holder.value().getIngredients()) {
                for (ItemStack option : ingredient.getItems()) {
                    if (option.getItem() instanceof CoinItem coin) {
                        madeFrom.computeIfAbsent(out.denom(), d -> new HashSet<>()).add(coin.denom());
                    }
                }
            }
        }
        CoinDenomination[] all = CoinDenomination.values();
        List<String> missing = new ArrayList<>();
        for (int i = 1; i + 1 < all.length; i++) { // skip NONE at 0
            CoinDenomination small = all[i], large = all[i + 1];
            if (!madeFrom.getOrDefault(large, Set.of()).contains(small)) missing.add(small + " up to " + large);
            if (!madeFrom.getOrDefault(small, Set.of()).contains(large)) missing.add(large + " down to " + small);
        }
        if (!missing.isEmpty()) helper.fail("no recipe for " + missing);
        helper.succeed();
    }

    /** Every state of the safe, whichever way it faces, is the banker's job site, and villagers may claim it. */
    @GameTest(template = ARENA)
    public static void theSafeIsTheBankersJobSite(GameTestHelper helper) {
        for (BlockState state : TradeBlocks.SAFE.get().getStateDefinition().getPossibleStates()) {
            var poi = PoiTypes.forState(state);
            assertTrue(helper, state + " to be the banker's job site",
                    poi.isPresent() && poi.get().value() == TradeVillagers.BANKER_POI.value());
        }
        assertTrue(helper, "the banker's job site to be acquirable",
                TradeVillagers.BANKER_POI.is(PoiTypeTags.ACQUIRABLE_JOB_SITE));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyItemHasAName(GameTestHelper helper) {
        JsonObject lang = readJson(helper, "/assets/erranttrade/lang/en_us.json");
        if (lang == null) return;
        List<String> missing = new ArrayList<>();
        for (Item item : ourItems()) {
            if (!lang.has(item.getDescriptionId())) missing.add(item.getDescriptionId());
        }
        if (!missing.isEmpty()) helper.fail("no translation for " + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyItemHasAModel(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (Item item : ourItems()) {
            String path = "/assets/erranttrade/models/item/" + BuiltInRegistries.ITEM.getKey(item).getPath() + ".json";
            if (ContentGameTests.class.getResource(path) == null) missing.add(path);
        }
        if (!missing.isEmpty()) helper.fail("no item model at " + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyItemIsInTheCreativeTab(GameTestHelper helper) {
        CreativeModeTab tab = TradeCreativeModeTabs.ITEMS_TAB.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, false, helper.getLevel().registryAccess()));
        Set<Item> shown = new HashSet<>();
        for (ItemStack stack : tab.getDisplayItems()) shown.add(stack.getItem());
        Set<Item> exempt = notInCreativeTab();
        List<String> missing = new ArrayList<>();
        for (Item item : ourItems()) {
            if (!shown.contains(item) && !exempt.contains(item)) {
                missing.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
        }
        if (!missing.isEmpty()) helper.fail("not in the creative tab: " + missing);
        helper.succeed();
    }

    /**
     * Every texture our models name, and every model our blockstates name, exists. A missing one
     * renders as the checkerboard and logs nothing: the coin pile's particle texture was missing.
     */
    @GameTest(template = ARENA)
    public static void everyModelAndTextureThatIsNamedExists(GameTestHelper helper) {
        Path assets = ModList.get().getModFileById(ErrantTrade.MOD_ID).getFile().findResource("assets", ErrantTrade.MOD_ID);
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(assets)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                String folder = assets.relativize(file).getName(0).toString();
                JsonObject json;
                try (var reader = Files.newBufferedReader(file)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
                if (folder.equals("models") && json.has("textures")) {
                    for (var ref : json.getAsJsonObject("textures").entrySet()) {
                        String value = ref.getValue().getAsString();
                        if (value.startsWith("#")) continue; // a reference to another texture slot
                        requireOurs(assets, value, "textures", ".png", file, missing);
                    }
                }
                if (folder.equals("blockstates") && json.has("variants")) {
                    for (var variant : json.getAsJsonObject("variants").entrySet()) {
                        String model = variant.getValue().getAsJsonObject().get("model").getAsString();
                        requireOurs(assets, model, "models", ".json", file, missing);
                    }
                }
            }
        } catch (java.io.IOException e) {
            helper.fail("could not walk the mod's assets: " + e);
            return;
        }
        if (!missing.isEmpty()) helper.fail("named but missing: " + missing);
        helper.succeed();
    }

    /** Only our own namespace can be checked; vanilla's assets are not on the server. */
    private static void requireOurs(Path assets, String id, String folder, String extension, Path from, List<String> missing) {
        String prefix = ErrantTrade.MOD_ID + ":";
        if (!id.startsWith(prefix)) return;
        Path expected = assets.resolve(folder).resolve(id.substring(prefix.length()) + extension);
        if (!Files.exists(expected)) missing.add(id + " (named by " + assets.relativize(from) + ")");
    }

    private static List<Item> ourItems() {
        return TradeItems.ITEMS.getEntries().stream().map(DeferredHolder::get).map(Item.class::cast)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private static JsonObject readJson(GameTestHelper helper, String path) {
        try (InputStream in = ContentGameTests.class.getResourceAsStream(path)) {
            if (in == null) {
                helper.fail("missing " + path);
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            helper.fail("could not read " + path + ": " + e);
            return null;
        }
    }
}
