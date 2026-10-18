# Working agreement for agents

Errant Trade and Markets (mod id `erranttrade`; formerly "Trade and Open Markets", TAOM): a NeoForge mod for Minecraft 1.21.1 that replaces the emerald
economy with multi-denomination coins, a wallet, coin piles you can place in the world, a safe,
identity cards, and a reskinned villager trading screen. [README](README.md) has the player-facing
feature list and the roadmap.

**Status, 17 Oct 2026: early alpha, written by hand. The test infrastructure is in place but
barely used: 3 unit tests and 1 gametest.** Agents started working on the code on 17 Oct, when the
mod was also renamed from Trade and Open Markets (`tradeandopenmarkets`). The near-term goal is
**testing first, then refactoring and cleanup**, in that order. Phase by phase plan and the bugs already found are in
[docs/BACKLOG.md](docs/BACKLOG.md). Keep this paragraph honest; it is the first thing anyone reads.

---

## 1. Ground rules for this phase

- **Characterise before you change.** Behaviour is protected by nothing today. Before refactoring
  a class, write the test that pins what it does now, including what it does wrong, and land that
  test in its own commit. Then fix or refactor in a separate commit that changes the assertion
  deliberately. A refactor and a behaviour change in one commit cannot be reviewed.
- **A bug found while testing goes in [BACKLOG](docs/BACKLOG.md) first.** Fix it in its own change
  unless it blocks the test you are writing. Coin loss and duplication bugs are the priority.
- **Do not add features** during this phase unless asked. The README's "Upcoming Work" is not a
  to-do list for agents.
- **Most of the comments are tutorial residue** ("your existing Codec", "pick one",
  "Fallback if your block doesn't use orientation"). Removing them is welcome cleanup; do not
  treat them as design intent.
- **Ask before making product decisions.** Denomination values, whether coins are placeable,
  wallet capacity, what shift does: those are the author's calls. The invariants in §5 are the
  ones the README states or the code clearly enforces.

## 2. Repo layout

Single-project Gradle build from the NeoForge MDK template.

```
CreateTradeAndOpenMarkets/
├─ build.gradle / gradle.properties   versions live in gradle.properties
├─ src/main/java/com/errantmc/markets/   the mod (mod id `erranttrade`)
│  │  ├─ ErrantTrade                  common entrypoint: every DeferredRegister + game-bus listeners
│  │  ├─ ErrantTradeClient            client entrypoint (@Mod dist=CLIENT): renderers, screens, tooltips
│  │  ├─ data/CoinDenomination        the denomination enum: value in copper bits, lang key, colour
│  │  ├─ component/                   data components: CurrencyData, IdentityCardData, WalletBalance helpers
│  │  ├─ item/                        coins, coin stack, wallet/, idcard/
│  │  ├─ block/                       coin block (pile), safe; entity/ holds BEs, renderer, left-click handler
│  │  ├─ screen/                      menus + screens: wallet/, safe/, shop/ (villager UI replacement)
│  │  ├─ network/                     payload registration; one message (identity card player sync)
│  │  ├─ registry/                    creative tab, sounds
│  │  ├─ villager/                    banker profession + POI (job block: the safe)
│  │  └─ Config                       COMMON config; still carries the MDK's example entries
│  └─ lib/                            small shared helpers (UI, codecs, noop model), formerly `errantlib`
├─ src/main/resources/                hand-written JSON: models, blockstates, recipes, tags, lang, curios
├─ src/main/templates/META-INF/       neoforge.mods.toml, expanded from gradle.properties
└─ resource_src/                      art sources (.xcf, .bbmodel), not shipped
```

There is **no datagen yet**: all JSON under `src/main/resources` is hand-written, though
`build.gradle` already wires `src/generated/resources` in. Unit tests live in `src/test/java`.
Gametests live in `src/main/java/.../gametest/`, because the game discovers them from the mod
itself, and their templates are in `data/erranttrade/structure/`.

The directory is named `CreateTradeAndOpenMarkets`, but **Create is not a dependency today**.
Its lines in `build.gradle` are commented out and `neoforge.mods.toml` lists it as optional.

## 3. Platform

| Thing | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.209 |
| ModDevGradle | 2.0.112 |
| Mappings | Parchment 2024.11.17 |
| Java target | 21 (Gradle toolchain, resolved by foojay) |
| Gradle | 8.14.3 (wrapper) |
| Optional deps | JEI 19.8.2.99 (runtime), Curios 9.5.1 (compileOnly API) |

**This is not the errant-metalworks platform.** That repo is on Minecraft 26.2, and most of its
platform notes are the reverse of what is true here. On 1.21.1, `ResourceLocation`, `GuiGraphics`,
`@GameTest`/`@GameTestHolder`, `Block.onRemove` and `BlockEntityRenderer.render(...)` all exist and
are correct. Do not port 26.x idioms into this code.

Facts that matter when writing code here:

- **Every package in this jar must be unique to this mod.** NeoForge loads mods as JPMS modules,
  and two mod jars that contain the same package refuse to load together. errant-metalworks also
  lives under `com.errantmc`, so everything here stays under `com.errantmc.markets`, including the
  helpers in `lib/`. Sharing code between the two means a real library jar (Jar-in-Jar), not
  copying classes into a common package.

- **Curios is `compileOnly`.** Any class that touches `top.theillusivec4.curios` must only reach
  it behind `ModList.get().isLoaded("curios")`, and preferably in a separate class so the JVM never
  links it when Curios is absent. `WalletAutoDeposit` currently inlines it; see BACKLOG.
- **Client-only code lives under `ErrantTradeClient` or classes only it references.**
  Renderers, screens, tooltip clients, `IdentityCardClientCache`. A common class that imports
  `net.minecraft.client` crashes a dedicated server. Run `runServer` after touching anything
  near that line.
- **The shop screen replaces vanilla's `MerchantScreen` on the client only**
  (`ScreenEvent.Opening`). The server still runs vanilla's `MerchantMenu`, so every trade the
  shop screen performs has to go through the vanilla menu's own slots and packets. A client-side
  shortcut there does nothing, or desyncs.
- **Registry objects are not available in plain unit tests.** Touching `TradeItems`, `ItemStack`,
  or a `DeferredHolder.get()` outside a running game fails in static init. Keep logic you want
  unit-tested free of those. `CurrencyData` is close: only `add(ItemStack)` and `getFrom(ItemStack)`
  tie it to the game.

Development is on Windows. The `java` on `PATH` is JDK 25; the build pins 21 with a toolchain, so
do not "fix" a Java version error by changing `JAVA_HOME`. CI (`.github/workflows/build.yml`)
runs `./gradlew build` on Ubuntu with Temurin 21.

## 4. Build and run

From PowerShell in the repo root:

| Command | Does |
|---|---|
| `.\gradlew.bat build` | compile, jar, and run the JUnit tests |
| `.\gradlew.bat test` | JUnit tests only, with FML booted (`neoForge.unitTest`) |
| `.\gradlew.bat runClient` | dev client with JEI |
| `.\gradlew.bat runServer` | dedicated server (`--nogui`). The check that client code has not leaked into common code |
| `.\gradlew.bat runGameTestServer` | runs every gametest and exits nonzero on failure. Needs no EULA. Search the log for `required tests` |
| `python tools/make-empty-template.py <name> x y z` | writes an empty gametest template into `data/erranttrade/structure/` |
| `.\gradlew.bat runData` | datagen into `src/generated/resources`. No providers exist yet |

**Do not build while a dev client is running.** A rebuild replaces classes under a running game
and the next screen or texture load crashes it with an error that names something else.

A cold build downloads and decompiles Minecraft, which takes several minutes the first time.

## 5. Invariants

These are what the README promises or the code clearly relies on. Breaking one is a decision for
the author, not a refactor.

| Invariant | Why |
|---|---|
| **Coins are conserved.** Every deposit, withdrawal, drop and pickup moves exactly as many coins as it removes. Nothing duplicates and nothing vanishes, including at `Integer.MAX_VALUE` | it is an economy mod; a dupe or a sink breaks the economy |
| **Plain coins carry no data components and stack to 64.** Only the coin stack item, the wallet and the ID card carry per-instance data | stacking; per-instance data on a bulk item makes inventories unmanageable |
| **Value is counted in copper bits** (`CoinDenomination.value`); display divides by 100 | every total and every recipe ratio follows from it |
| **Mutation is server-side.** Clients request through menu buttons (`clickMenuButton`) or interactions; the server changes balances | anything else is a dupe exploit on a server |
| **A large coin pile breaks into one coin stack item**, not thousands of item entities | the README's stated reason for the coin stack item: lag and crashes |
| **Optional mods stay optional.** The mod loads and works without JEI, Curios or Create | the README says no hard dependencies |
| **Data component and NBT formats are save formats.** Renaming a codec field (`coinsCopperBit` …) or a component id loses players' money on upgrade | write a migration or keep the old name |

## 6. Conventions

What the code already does, and what to converge on during cleanup:

- **Registration** is one `Trade*` class per registry (`TradeItems`, `TradeBlocks`, `TradeBlockEntities`,
  `TradeComponents`, `TradeMenus`, `TradeSounds`, `TradeVillagers`, `TradeCreativeModeTabs`), each with a
  `register(IEventBus)` called from the mod constructor. A new registry follows the same shape.
- **Item state is a `DataComponentType` with a `Codec` and a `StreamCodec`.** `CurrencyData` is the
  worked example.
- **Denomination dispatch should go through `CoinDenomination`.** Today `CoinItem`, `CoinBlock`
  and `CurrencyData` each repeat an eight-way `if` or `switch` over the coins. Adding a
  denomination means editing all of them, and missing one fails silently. Collapsing these is
  backlog item one for refactoring.
- **Logging:** a class-level slf4j `Logger` from `LogUtils.getLogger()`. Not `System.err`, and no
  `LOG.info` on per-interaction or per-tick paths (the coin block logs at INFO on every click).
- **Player-facing text is translatable** (`Component.translatable`, keys in `en_us.json`). Several
  tooltips and the safe's title are still `Component.literal`.
- **Money formatting** currently uses `NumberFormat.getCurrencyInstance()`, which follows the
  JVM locale; a German client sees euros. Converge on one formatter that does not.
- Comments say why, not what. Remove MDK and tutorial boilerplate as you touch a file.

## 7. Testing

Two layers, both run in CI. What still needs covering is in [BACKLOG §Testing](docs/BACKLOG.md#testing).

1. **JUnit unit tests** (`src/test/java`) for pure logic: `CurrencyData` arithmetic and overflow,
   denomination lookup, coin-stack tier, change making. `neoForge.unitTest` boots FML, so a test
   *may* touch registries, but prefer extracting the logic so it doesn't need to.
   `CurrencyDataTest` is the example.
2. **NeoForge gametests** for in-world behaviour: coin block insert, withdraw, break and drop,
   wallet auto-deposit, safe drops. On 1.21.1 a holder class carries
   `@GameTestHolder(ErrantTrade.MOD_ID)` and `@PrefixGameTestTemplate(false)`, and each test is a
   static `@GameTest(template = GameTests.ARENA)` method. `CoinBlockGameTests` is the example.
   Build inside the 9×9×9 arena; for anything bigger, make a bigger template with the tool.
   Prefer `succeedWhen` over waiting a fixed number of ticks and then asserting.

**Before trusting a new test, make it fail once.** Break the expectation, check that
`runGameTestServer` reports it, then put it back. `CoinBlockGameTests` was checked this way.

**A test that calls a helper directly proves the helper, not the feature.** For the coin block,
break the block in the gametest and count the item entities, instead of calling `onRemove`.

Any list that has to match a registry (the denomination switches, the creative tab, lang keys,
item models) should get a test saying so. Those drift silently: a missing translation shows the
raw key and a missing model shows the purple-black checkerboard, and neither logs anything.
