# Working agreement for agents

Errant Trade and Markets (mod id `erranttrade`; formerly "Trade and Open Markets", TAOM): a
NeoForge mod for Minecraft 1.21.1 that replaces the emerald economy with multi-denomination coins,
a wallet, coin piles you can place in the world, a safe, identity cards, and (work in progress) a
reskinned villager trading screen. [README](README.md) has the player-facing feature list and the
roadmap.

**Status, 17 Oct 2026: early alpha, written by hand. 22 unit tests (19 of them in the
Minecraft-free domain module) and 58 gametests, run with and without Curios. Every bug found by
both audits is fixed** ([BACKLOG](docs/BACKLOG.md)). Agents started on the code on 17 Oct, which
is also when the mod was renamed from Trade and Open Markets (`tradeandopenmarkets`) and its
registry IDs were tidied. **No world from before that loads.**

**This branch (`feature/npc-interactions`) also carries the trader overhaul**: right-clicking a
villager opens the mod's own shop instead of vanilla's trading screen, and buying and selling work
there, in coins. [docs/design/trading.md](docs/design/trading.md) records how it works and what was
decided; what is still missing (discounts, buying several at once) is in its §8. Keep
this paragraph honest; it is the first thing anyone reads.

---

## 1. Ground rules

- **Characterise before you change.** Before refactoring a class, make sure a test pins what it
  does now, including what it does wrong, and land that test in its own commit. Then fix or
  refactor in a separate commit that changes the assertion deliberately. A refactor and a
  behaviour change in one commit cannot be reviewed.
- **A bug found while testing goes in [BACKLOG](docs/BACKLOG.md) first.** Fix it in its own change
  unless it blocks the test you are writing. Coin loss and duplication bugs are the priority.
- **Do not add features** unless asked. The README's "Upcoming Work" is not a to-do list for
  agents.
- **Many comments are tutorial residue** ("your existing Codec", "pick one"). Removing them is
  welcome cleanup; do not treat them as design intent.
- **Ask before making product decisions.** Denomination values, whether coins are placeable,
  wallet capacity, what shift does: those are the author's calls. The invariants in §5 are the
  ones the README states, the code clearly enforces, or the author has decided.

## 2. Repo layout

A two-project Gradle build: the mod, and a domain library with no Minecraft in it.

```
CreateTradeAndOpenMarkets/
├─ build.gradle / gradle.properties   the mod's build; versions live in gradle.properties
├─ settings.gradle                    includes errant-trade-domain
├─ errant-trade-domain/               NO MINECRAFT: the economy as plain Java
│  └─ src/main/java/com/errantmc/markets/domain/
│        CoinDenomination              the coins and their value in copper bits
│        Purse                         an amount of coins; conservation and overflow rules
│        MoneyFormat                   the one money formatter ($1,234.56, any locale)
│        PileLayout                    where a coin pile's columns stand
├─ src/main/java/com/errantmc/markets/   the mod (mod id `erranttrade`)
│  ├─ ErrantTrade                     common entrypoint: every DeferredRegister + game-bus listeners
│  ├─ ErrantTradeClient               client entrypoint (@Mod dist=CLIENT): renderers, screens, tooltips
│  ├─ Config                          COMMON config; still has MDK example entries
│  ├─ component/                      data components: Purses (the purse codec + stack access), identity cards
│  ├─ item/                           coins (CoinItem, CoinDeposits, CoinStyle), coin stack, wallet/, idcard/
│  ├─ block/                          coin pile (CoinBlock), safe; entity/ holds BEs, renderer, left-click handler
│  ├─ npcinteractions/                the trader overhaul (in progress): interaction tabs, their registry
│  │                                  and data loader (data/erranttrade/npcinteractions/*.json)
│  ├─ screen/                         menus + screens: wallet/, safe/, villager/ (the interaction menu)
│  ├─ network/                        payload registration; one message (identity card data)
│  ├─ compat/CuriosCompat             the only class that may name Curios
│  ├─ registry/                       creative tab, sounds
│  ├─ villager/                       banker profession + POI (job site: the safe)
│  └─ lib/                            small helpers (UI button, noop model), formerly `errantlib`
├─ src/gametest/                      gametests and their templates. Part of the mod in dev runs, not in the jar
├─ src/test/                          JUnit tests that need FML (the save format)
├─ src/main/resources/                hand-written JSON: models, blockstates, recipes, tags, lang, curios
├─ src/main/templates/META-INF/       neoforge.mods.toml, expanded from gradle.properties
├─ tools/make-empty-template.py       writes empty gametest templates
└─ resource_src/                      art sources (.xcf, .bbmodel), not shipped
```

There is **no datagen yet**: all JSON under `src/main/resources` is hand-written, though
`build.gradle` already wires `src/generated/resources` in.

The directory is named `CreateTradeAndOpenMarkets`, but **Create is not a dependency today**.

### The domain module

**`errant-trade-domain` declares no Minecraft dependency, and that is the point.** It makes
"no Minecraft here" a compile error rather than a review comment. Put logic there when it is about
money rather than about Minecraft: arithmetic, rules, formatting, geometry. The mod keeps thin
adapters (`Purses` for codecs and `ItemStack` access, `CoinItem.forDenomination`, `CoinStyle` for
colours).

It is wired in three places in `build.gradle`, and all three are needed:
- `compileOnly(project(':errant-trade-domain'))`, so the mod compiles against it;
- `sourceSet(project(':errant-trade-domain').sourceSets.main)` in the `mods` block, so dev runs,
  gametests and unit tests load its classes **as part of the mod's own module**;
- `jar { from(...domain output) }`, so the release jar carries them.

It must not also be an `implementation` dependency: a second copy on the runtime classpath splits
the `com.errantmc.markets.domain` package across two modules and NeoForge refuses to start.

## 3. Platform

| Thing | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.209 |
| ModDevGradle | 2.0.112 |
| Mappings | Parchment 2024.11.17 |
| Java target | 21 (Gradle toolchain, resolved by foojay) |
| Gradle | 8.14.3 (wrapper) |
| Optional deps | JEI 19.8.2.99 and Curios 9.5.1 in dev runs; neither is required |

**This is not the errant-metalworks platform.** That repo is on Minecraft 26.2, and most of its
platform notes are the reverse of what is true here. On 1.21.1, `ResourceLocation`, `GuiGraphics`,
`@GameTest`/`@GameTestHolder`, `Block.onRemove` and `BlockEntityRenderer.render(...)` all exist and
are correct. Do not port 26.x idioms into this code.

Facts that matter when writing code here:

- **Every package in this jar must be unique to this mod.** NeoForge loads mods as JPMS modules,
  and two mod jars that contain the same package refuse to load together. errant-metalworks also
  lives under `com.errantmc`, so everything here stays under `com.errantmc.markets`, the domain
  and `lib/` included. Sharing code between the two means a real library jar (Jar-in-Jar), not
  copying classes into a common package.
- **Curios is optional.** Only `compat/CuriosCompat` (and gametests) may import
  `top.theillusivec4.curios`, and only after `CuriosCompat.isLoaded()`. Curios accepts an item
  into a slot when it is in the `curios:<slot>` item tag; ours is `data/curios/tags/item/wallet.json`.
- **Client-only code lives under `ErrantTradeClient` or classes only it references**: renderers,
  screens, tooltip clients, `IdentityCardClientCache`. A common class that imports
  `net.minecraft.client` crashes a dedicated server.
- **A screen and the server must run the same menu.** Every click a screen sends is applied to
  the server's open menu by slot index, so a screen over a client-side copy of a menu clicks the
  wrong slots (BACKLOG #18). The villager interaction is built the right way round: the server
  cancels the vanilla interaction and opens `VillagerInteractionMenu` itself
  (`VillagerInteractionEvents`), and the client's copy is built from the open packet. Keep it so.
- **Menus and the objects they build are common code.** They run on the dedicated server, which
  refuses to load `Minecraft` or anything else under `net.minecraft.client`. Look entities up with
  `Level.getEntity`, not the client level (this crashed every villager interaction on a server
  until 18 Oct). The interaction tabs still mix drawing code into objects the server builds; see
  the backlog.
- **A mod packet to a player whose connection lacks our channel throws.** Fake players from other
  mods are like that. Send through `TradeNetwork.sendTo`, which checks `hasChannel`.
- **A block that requires the correct tool for drops must be in a `mineable/*` tag**, or no tool is
  correct and it never drops (BACKLOG #15).
- **Registry objects are not available outside a running game.** Touching `TradeItems`,
  `ItemStack` or a `DeferredHolder.get()` in a plain JUnit test fails in static init. Domain tests
  never need them; mod unit tests run under `neoForge.unitTest`, which boots FML first.

Development is on Windows. The `java` on `PATH` is JDK 25; the build pins 21 with a toolchain, so
do not "fix" a Java version error by changing `JAVA_HOME`. The repo stores CRLF line endings
(`core.autocrlf=true`): a script that rewrites files must preserve them, or every line of every
file it touches shows as changed.

## 4. Build and run

From PowerShell in the repo root:

| Command | Does |
|---|---|
| `.\gradlew.bat build` | compile, jar, and run every JUnit test (domain and mod) |
| `.\gradlew.bat :errant-trade-domain:test` | the domain's tests alone: no Minecraft, a few seconds |
| `.\gradlew.bat test` | the mod's JUnit tests, with FML booted (`neoForge.unitTest`) |
| `.\gradlew.bat runGameTestServer` | every gametest; exits nonzero on failure. Needs no EULA. Search the log for `required tests` |
| `.\gradlew.bat runGameTestServer -PnoCurios` | the same without Curios, which is how "optional mods stay optional" is tested |
| `.\gradlew.bat runClient` | dev client with JEI and Curios |
| `.\gradlew.bat runServer` | dedicated server (`--nogui`); needs `run/eula.txt` |
| `python tools/make-empty-template.py <name> x y z` | writes an empty gametest template into `src/gametest/resources/data/erranttrade/structure/` |
| `.\gradlew.bat runData` | datagen into `src/generated/resources`. No providers exist yet |

CI (`.github/workflows/build.yml`, Ubuntu, Temurin 21) runs `build`, then the gametests with and
without Curios.

**Do not build while a dev client is running.** A rebuild replaces classes under a running game,
and the next screen or texture load crashes it with an error that names something else.

A cold build downloads and decompiles Minecraft, which takes several minutes the first time.

## 5. Invariants

These are what the README promises, the code clearly relies on, or the author has decided.
Breaking one is a decision for the author, not a refactor.

| Invariant | Why |
|---|---|
| **Coins are conserved.** Every deposit, withdrawal, drop and pickup moves exactly as many coins as it removes. Nothing duplicates and nothing vanishes, including at `Integer.MAX_VALUE`. `Purse` enforces it: single coins move what fits, whole purses merge only after `canAdd` | it is an economy mod; a dupe or a sink breaks the economy |
| **Deposits go through `CoinDeposits.absorb`.** One take-what-fits routine for the pile, the wallet screen, its grid and pickup | four copies of this rule caused bugs 1–3 |
| **Plain coins carry no data components and stack to 64.** Only the coin stack item, the wallet and the ID card carry per-instance data | stacking; per-instance data on a bulk item makes inventories unmanageable |
| **Value is counted in copper bits** (`CoinDenomination.value`); display divides by 100, always through `MoneyFormat` | every total and every recipe ratio follows from it; `ContentGameTests` checks the recipes |
| **Mutation is server-side.** Clients request through menu buttons (`clickMenuButton`) or interactions; the server changes balances | anything else is a dupe exploit on a server |
| **An open wallet cannot move.** Its slot in its own menu is locked against pickup, throwing and number-key or offhand swaps, and its grid is written through as it changes | moving it was a duplication (BACKLOG #16) |
| **Only wallet things fit in a wallet**: the `erranttrade:wallet_items` tag (currency, ID cards, debit cards). Never containers | author's decision; nested storage and oversized item data (BACKLOG #19) |
| **A player is sent live identity-card data only for cards they carry** (inventory, a wallet, a curio), never broadcast | a snapshot holds its subject's position and respawn point (BACKLOG #17) |
| **The safe is hard to break on purpose**: destroy time 1200, a diamond pickaxe to drop it | author's decision |
| **Trades happen on the server only.** The client shows the server's tabs and sends a menu button; `ShopTrades` checks stock, items and money before anything moves | [trading §1](docs/design/trading.md#1-who-decides-what) |
| **Paying takes loose coins first, then the wallet; income goes to the wallet if carried** | author's decision; [trading §3](docs/design/trading.md#3-paying-and-being-paid) |
| **A trade moves money between the player and the villager's till; it never creates or destroys it.** A villager refuses to buy when its till is short | `EconomyGameTests`; [economy §3](docs/design/economy.md#3-the-proposal-every-npc-has-a-balance) |
| **Experience scales with a trade's coin value**, for the villager and the player alike; never a fixed amount per trade | author's decision; a fixed amount is farmable; [trading §5](docs/design/trading.md#5-progression) |
| **A large coin pile breaks into one coin stack item**, not thousands of item entities | the README's stated reason for the coin stack item: lag and crashes |
| **Optional mods stay optional.** The mod loads and works without JEI, Curios or Create | the README says no hard dependencies; CI runs without Curios |
| **Data component and NBT formats are save formats.** `Purses.CODEC` keeps `CurrencyData`'s field names (`coinsCopperBit` …) and the component keeps the id `currency_data`. Renaming either loses players' money | `PursesCodecTest` reads an old save. Write a migration or keep the old name |
| **The domain module has no Minecraft dependency** | §2 |

## 6. Conventions

- **Registration** is one `Trade*` class per registry (`TradeItems`, `TradeBlocks`,
  `TradeBlockEntities`, `TradeComponents`, `TradeMenus`, `TradeSounds`, `TradeVillagers`,
  `TradeCreativeModeTabs`), each with a `register(IEventBus)` called from the mod constructor.
- **Registry IDs** name the thing plainly (`safe`, `coin_pile`, `leather_wallet`); asset, loot
  table and texture files follow their IDs.
- **Denominations are never listed by hand.** Loop over `CoinDenomination.coins()`; get a coin's
  item with `CoinItem.forDenomination` and an item's denomination with `CoinItem.denom()`. Adding a
  denomination is then a domain change, an item, assets and recipes, and the tests say what is
  missing.
- **Item state is a `DataComponentType` with a `Codec` and a `StreamCodec`**; `Purses` is the
  worked example.
- **Logging:** a class-level slf4j `Logger` from `LogUtils.getLogger()`. Not `System.out`/`err`,
  and nothing at INFO on per-interaction or per-tick paths.
- **Player-facing text is translatable** (`Component.translatable`, keys in `en_us.json`). The
  safe's title is still a literal.
- Comments say why, not what. Remove MDK and tutorial boilerplate as you touch a file.

## 7. Testing

Three layers, all run in CI. What is still uncovered is in
[BACKLOG §Testing](docs/BACKLOG.md#testing).

1. **Domain tests** (`errant-trade-domain/src/test`): plain JUnit, no Minecraft, milliseconds.
   The first place for anything about money: `PurseTest`, `MoneyFormatTest`, `PileLayoutTest`.
2. **Mod unit tests** (`src/test`): JUnit under `neoForge.unitTest`, which boots FML first (about
   ten seconds). For what needs Minecraft types but no world: `PursesCodecTest` pins the save
   format.
3. **Gametests** (`src/gametest`) for in-world behaviour. On 1.21.1 a holder class carries
   `@GameTestHolder(ErrantTrade.MOD_ID)` and `@PrefixGameTestTemplate(false)`, and each test is a
   static `@GameTest(template = GameTests.ARENA)` method. Build inside the 9×9×9 arena; for
   anything bigger, make a bigger template with the tool. Prefer `succeedWhen` over waiting a
   fixed number of ticks and then asserting.

   For a player, use `GameTests.survivalPlayer(helper)` (or `mockPlayer(helper, mode)`). It joins
   through the real login path inside the test's own arena. Drive it with
   `helper.useBlock(pos, player)`, `item.use(...)`, `entity.playerTouch(player)`, `menu.clicked(...)`,
   or by posting the NeoForge event the server would fire. Never use
   `helper.makeMockServerPlayerInLevel()`:
   - **its player is creative**, and creative inventories are never charged, so a deposit test
     passes while moving nothing;
   - **its connection negotiated no mod channels**, so the first mod to send it a payload on login
     throws (Curios does). `mockPlayer` uses NeoForge's `NetworkRegistry.configureMockConnection`;
   - **it joins at the world spawn**, where what one test drops is counted by another.

   **Curios tests** live in `CuriosGameTests` and start with
   `if (!CuriosCompat.isLoaded()) { helper.succeed(); return; }`, so they pass trivially in the
   `-PnoCurios` run.

**Before trusting a new test, make it fail once.** Break the expectation (or stash the fix), check
that the run reports it, then put it back.

**A test that calls a helper directly proves the helper, not the feature.** For the coin pile,
break the block in the gametest and count the item entities, instead of calling `onRemove`.

**Any list that has to match a registry gets a test saying so.** `ContentGameTests` checks that
every item has a name, a model and a creative tab entry, that every texture and model our assets
name exists, that every denomination has one coin item, that coin recipes conserve value, and that
the safe is the banker's job site. These drift silently otherwise: a missing translation shows the
raw key, a missing model or texture the purple-black checkerboard, and neither logs anything.
