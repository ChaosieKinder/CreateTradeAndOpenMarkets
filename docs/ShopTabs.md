# Extending Trade & Open Markets Shop Tabs

Trade & Open Markets exposes its merchant UI through a lightweight tab framework so that data packs and other mods can mix and match different trading experiences. This document covers how to plug additional tab types into the system and how professions resolve their tab layouts.

## Runtime registration

Every tab implementation provides a [`ShopTabType`](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/ShopTabType.java) instance. A type advertises a unique `ResourceLocation` identifier and builds concrete [`ShopTab`](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/ShopTab.java) objects when a shop definition asks for it.

Custom types must be registered during common setup so the loader can find them later:

```java
public final class MyModShopTabs {
    public static final ResourceLocation ENCHANT = MyMod.id("enchant");

    public static void init() {
        ShopTabRegistry.register(new EnchantingTabType());
    }
}
```

The registry lives entirely on the runtime side—registration is just a static method call. Attempts to reuse an identifier will throw, helping catch duplicate IDs at startup.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/tab/ShopTabRegistry.java†L1-L37】

### Implementing a tab type

`ShopTabType#create` receives two helpers:

* [`ShopTabDefinition`](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/config/ShopTabDefinition.java) – the parsed JSON node for this tab. It exposes the tab type ID, the localized title component, and an arbitrary `JsonObject` payload you can interpret however you like.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/tab/config/ShopTabDefinition.java†L1-L11】
* [`ShopTabGenerationContext`](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/ShopTabGenerationContext.java) – contextual information about the current merchant, including the villager (if present), their vanilla offers, world/day info, and a seeded `RandomSource` so tabs can roll deterministic daily inventory.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/tab/ShopTabGenerationContext.java†L1-L18】

A simple implementation might deserialize JSON into `ShopEntry` objects and return a custom `ShopTab` subclass, as the built-in [buy](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/types/BuyShopTabType.java) and [sell](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/tabs/types/SellShopTabType.java) types do.

## Data-driven profession layouts

Shop layouts are defined through data packs under `data/<namespace>/shops/<profession>.json`. The [`ShopDefinitionLoader`](../src/main/java/net/errantchaosie/tradeandopenmarkets/npcinteractions/ShopDefinitionLoader.java) watches this folder, parses each file into tab definitions, and invalidates cached UIs whenever the data pack set changes.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/ShopDefinitionLoader.java†L19-L78】

Each file accepts:

* `profession` (optional) – overrides the villager profession key inferred from the file name.
* `tabs` – ordered array of tab descriptors. Every entry requires a `type` ID and may supply a custom `title`. The `data` object is forwarded to the tab type untouched so custom implementations can define their own schema.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/ShopDefinitionLoader.java†L57-L76】

If a profession has no custom definition, the `ShopTabManager` falls back to vanilla offers by deriving simple buy and sell tabs on the fly.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/ShopTabManager.java†L64-L118】 Tabs produced from JSON are cached per villager and day, preventing trades from rerolling after players interact with them.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/ShopTabManager.java†L32-L63】

## Built-in schema examples

The default banker configuration shows the minimal fields needed to stand up a buy/sell pair, including stock ranges and localized titles.【F:resource_src/main/resources/data/tradeandopenmarkets/shops/banker.json†L1-L57】 The farmer definition demonstrates a larger assortment with weighted stock rolls, buyback support, and profession-specific pricing.【F:resource_src/main/resources/data/tradeandopenmarkets/shops/farmer.json†L1-L216】

Auto buybacks can be enabled on sell tabs by setting `auto_buybacks.enabled` and, optionally, a `percentage`. When active, any items sold back are added to linked buy tabs and priced at the configured fraction of the original cost.【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/tab/types/SellShopTabType.java†L32-L83】【F:src/main/java/net/errantchaosie/tradeandopenmarkets/shop/ShopTabManager.java†L118-L157】

Feel free to extend the JSON schema for your own tab types—the loader never validates the `data` payload beyond confirming it is a JSON object. This keeps the framework flexible while still letting different mods coexist under the same shop UI.
