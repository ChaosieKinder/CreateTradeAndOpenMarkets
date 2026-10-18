# Backlog: testing, bugs, cleanup

Opened 17 Oct 2026 from a first read of the whole codebase.

## Bugs

All twelve were fixed on 17 Oct. The first eleven were found by reading the code. Bugs 1–6 (and
#3 at the pile) were reproduced by a test that pinned the wrong behaviour before the fix, and all
of them behaved as predicted. #7's test was seen to fail without its fix. #9 replaced its
mechanism outright, so its tests were written against the new one. Each fix is its own commit,
with the bug's number in the subject. The record stays
here so the reasoning is not lost, and new bugs are appended with the next number.

### Coin conservation

| # | Bug | Fix | Covered by |
|---|---|---|---|
| 1 | The coin pile's sneak double tap added every stack to the pile's *original* balance, then wrote that original back: the inventory emptied, the pile did not change. The offhand was also counted twice (visible only in creative) | running total; one pass over `getContainerSize()` | `CoinBlockGameTests.sneakDoubleTap…` |
| 2 | The wallet's deposit-all passed inventory indices to `quickMoveStack`, which takes menu slot indices, so a coin stack in hotbar slot *i* moved wallet slot *i* out | deposit-all and the grid sweep share `WalletMenu.absorb`, straight into the balance | `WalletGameTests.depositAll…` |
| 3 | Coins past `Integer.MAX_VALUE` vanished: merges wrapped negative, and the wallet paths ignored what did not fit | `CurrencyData.canAdd`; merges refused whole; single coins leave the remainder in place; `add(CurrencyData)` throws rather than wrap | `CurrencyDataTest`, `CoinBlockGameTests.coinStackIntoAFullPile…`, `WalletGameTests.*NearlyFull*` |
| 4 | `CurrencyData.totalCount()` summed `int`s before widening. (The coin tooltip was suspected too, but 64 × 100,000 fits.) | widen first | `CurrencyDataTest` |
| 5 | Placing a coin always set the block at `pos.above()`, overwriting whatever was there | places like a block item: against the clicked face, only where the pile survives. **Clicking the side of a block no longer puts the pile on top of it** | `CoinBlockGameTests.placingACoin…` |
| 6 | The coin stack placed piles where they could not survive, and the next neighbour update dropped the whole balance; it also crashed without a player | `canSurvive` check, null player handled | `CoinBlockGameTests.coinStack…` |
| 7 | `SafeBlockEntity.clearContents` cleared slot 0 once per slot | clear each slot | `SafeGameTests` |

### Robustness

| # | Bug | Fix | Covered by |
|---|---|---|---|
| 8 | `WalletAutoDeposit`, which always loads, imported Curios (`compileOnly`) directly | `compat/CuriosCompat` is the only class that names Curios | none: Curios is not in the test runtime |
| 9 | Auto-deposit scanned for coin entities around every player every tick, and ignored an item's pickup target | handles `ItemEntityPickupEvent.Pre`, honouring the pickup delay and target | `WalletGameTests.picked…`, `coinsStill…` |
| 10 | `CoinBlock.LAST_SNEAK_USE` grew by one entry per player for the server's lifetime | expired taps are pruned on each tap | none: not observable without reflection |
| 11 | INFO logging on every coin block interaction and wallet button press | removed | none |
| 12 | The identity card sync threw for any player without our network channel (fake players, gametest players), on every login and every 1200 ticks. Found by the first gametest with a player | `TradeNetwork.sendTo` checks `hasChannel` | every gametest that makes a player |

### Found by the gameplay tests

| # | Bug | Fix | Covered by |
|---|---|---|---|
| 13 | The wallet could not be worn in the Curios wallet slot, despite the README. Curios accepts items in the `curios:wallet` tag, and the mod had tagged its wallets `erranttrade:wallet` | `data/curios/tags/item/wallet.json` includes `#erranttrade:wallet` | `CuriosGameTests.theWalletFitsTheWalletSlot` |
| 14 | The coin stack had no name and showed as `item.erranttrade.coin_stack` | lang entry "Coin Stack" | `ContentGameTests.everyItemHasAName` |

## Testing

### Phase 1: test infrastructure
Done 17 Oct.
- [x] `src/test/java` with JUnit 5, run by `build`. Uses `neoForge.unitTest`, so FML boots and
      tests may touch registries. Keep logic pure where you can anyway.
- [x] Gametest holder (`CoinBlockGameTests`) and an empty 9×9×9 `arena` template from
      `tools/make-empty-template.py`.
- [x] CI runs `runGameTestServer` after `build`, then again with `-PnoCurios`. Curios is in the
      dev and test runtime (`localRuntime`).

### Phase 2: gameplay tests for existing behaviour
Mostly done 17 Oct, 46 gametests in total:
- [x] Coin pile: placing; depositing (right-click, coin stack, double tap); withdrawing (one coin,
      a stack, the last coin); breaking (support, small pile, big pile into one coin stack,
      creative); overflow; saving and loading.
- [x] Wallet: auto-deposit (inventory, Curios, pickup delay, overflow); withdraw (one, a stack,
      empty denomination, full inventory); deposit-all; the grid (coins become balance, contents
      saved on close, no wallet inside a wallet); balance through saving.
- [x] Safe: drops when broken, 25-per-slot limit, faces the player, opens its menu, clearContents.
- [x] ID card: binds on sneak-use, not otherwise, and never rebinds.
- [x] Content: every coin recipe conserves value, every denomination converts both ways, the
      safe is the banker's job site, and every item has a name, a model and a creative tab entry.

Still to do:
- [x] Unit: the purse codec pins the save format (`PursesCodecTest`), and the domain covers
      `Purse`, `MoneyFormat` and the coin pile layout.
- [ ] `CoinStackItem.tier` thresholds.
- [ ] A banker villager actually claiming a safe. That needs a villager and many ticks, so it is
      slow and possibly flaky. The POI wiring is covered.
- [ ] Client-only behaviour (the shop screen replacing the merchant UI, tooltips, renderers) has
      no automated coverage, because gametests run on a dedicated server.

### Noticed while writing them (not bugs, but worth a decision)
- The README says right-click deposits coins and "shift for a whole stack", but a plain
  right-click has always deposited the whole held stack. The test pins what the code does.

## Audit, 17 Oct (second pass)

A second read of the whole codebase after the gameplay tests went in. Everything it found is
fixed. #15 and #16 were pinned by tests before their fixes; #17–#19 were decided by the author
first.

### Bugs it found

| # | Bug | Fix | Covered by |
|---|---|---|---|
| 15 | **A broken safe was lost.** It required the correct tool for drops but was in no `mineable/*` tag, so no tool was correct | `mineable/pickaxe` and `needs_diamond_tool`. Its destroy time of 1200 stays: a safe is meant to be hard to break into (author) | `SafeGameTests.aDiamondPickaxeIsTheRightToolForASafe`, `breakingASafeWithADiamondPickaxeDropsTheSafe` |
| 16 | **Wallet duplication.** The open wallet sat in an ordinary slot of its own menu and the grid was written back on close only if it was still in hand, so taking an item out and then throwing or moving the wallet left both | the wallet's slot is locked, number-key and offhand swaps cannot target it, and the grid is written through on every change | `WalletGameTests.throwingTheOpenWalletDoesNotDuplicateItsGrid`, `aNumberKeyCannotSwapTheOpenWalletAway`, `theGridIsSavedAsItChanges` |
| 17 | **The identity card sync broadcast every player's location**, respawn point, health and playtime to every client, every 60 s | each player is sent snapshots only for the cards they carry (inventory, inside a wallet, worn as a curio). The client cache is replaced by each sync and cleared on leaving a server. Binding a card sends its data at once (author) | `IdentityCardGameTests.aPlayerWithoutCardsIsSentNoOnesData`, `carryingSomeonesCardSendsOnlyTheirData`, `aCardInsideAWalletCounts` |
| 18 | **The shop screen clicked a different menu than the server's**: a client-only `ShopMenu` with its own slots, over the server's vanilla `MerchantMenu` | `ShopScreen` now draws over the `MerchantMenu` itself; `ShopMenu` and its broken `SHOP_MENU` registration are gone. The unfinished layout does not match vanilla's slot positions, so the swap is behind `ClientConfig.replaceTradingScreen`, off by default | none: client-only. The client was started and loads cleanly |
| 19 | **The wallet grid accepted any item**, containers included | only `erranttrade:wallet_items` (currency, ID cards, debit cards); deposit-all sweeps `currency_items` only (author) | `WalletGameTests.theGridTakesOnlyWalletThings`, `depositAllLeavesIdCardsInTheInventory` |
| 20 | **The coin pile's particle texture did not exist**, so breaking a pile threw missing-texture particles. Found while renaming its ID | it uses the gold mark's texture | `ContentGameTests.everyModelAndTextureThatIsNamedExists` (seen failing first) |

### Smaller defects

Fixed: `ShopScreen` shadowing `leftPos`/`topPos` (gone with the rewrite); `InventoryHelper`
mixing common constants with client drawing (deleted: constants into `SafeMenu`, drawing into
`ShopScreen`); `IdentityCardClientCache` never cleared; `IdentityCardItem`'s client imports; `§`
colour codes (`CoinStyle`); the Buy/Sell and coin stack literals; the coin pile renderer laying
out every pile every frame (cached per balance).

Still open:
- `WalletTooltip.createClientTooltipComponent` and `IdentityCardTooltip.createClientTooltipComponent`
  are dead and name client types from common classes.
- The safe's title is a literal ("Safe").

### Structure: the `-domain` module (done)

`errant-trade-domain` is a plain `java-library` with no Minecraft dependency, holding
`CoinDenomination`, `Purse` (replacing `CurrencyData`), `MoneyFormat` and `PileLayout`. Its 19
tests run in milliseconds. How it is wired, and why it must stay Minecraft-free, is in
[AGENTS §2](../AGENTS.md#the-domain-module). New logic about money starts there: change-making,
bank balances, note values, trader pricing, inflation detection.

**Not done, on purpose: a library shared with errant-metalworks.** It is on Minecraft 26.2 and
this mod is on 1.21.1, so no Minecraft-facing code can be shared, and the two mods have no domain
logic in common. What is shared is ideas: the gametest player, the registry-list tests, the empty
template tool. Copy those. Revisit this if the two mods ever share a Minecraft version, or if an
economy API for other mods to call becomes a goal (that would be its own `-api` artifact).

### Build and tests

- [x] Gametests no longer ship to players: they live in a `gametest` source set.
- [x] Registry IDs tidied while worlds were already broken: `safe`, `coin_pile`,
      `leather_wallet`, the block entities `safe` and `coin_pile`, the POI `banker`, the menus
      `safe` and `wallet`. **From the first public release on, every rename needs a migration.**
- [ ] **Datagen for what is derivable.** The 18 coin recipes follow from `CoinDenomination`
      values, so generate them, and the value test becomes a check on the generator. Lang, item
      models and tags are small but would stop drifting. The `src/generated/resources` wiring
      already exists.

## Refactoring and cleanup

- [x] **One source of truth for denominations.** `CoinDenomination.coins()` plus
      `CoinItem.forDenomination` / `denom()`. The eight-way switches are gone from `CoinItem`,
      `CoinBlock`, the renderer, the wallet screen and menu.
- [x] `CurrencyData` split into the domain `Purse` and the mod's `Purses` adapter. The save format
      is unchanged (`PursesCodecTest`).
- [x] One deposit routine: `CoinDeposits.absorb`.
- [x] `System.out`/`err` replaced by the logger, or deleted along with the code around them.
- [x] Locale-independent money formatting (`MoneyFormat`).
- [x] `getHighestDenomonation` is `Purse.highest()`.
- [ ] Remove the MDK example config entries (`logDirtBlock`, `magicNumber` …) and the unused
      `validateItemName`. **This changes the config file**, so mention it in the changelog.
- [ ] Remove dead code: `TradeComponents.register(String, …)`, commented-out blocks in the mod
      constructor, `if(false && …)` in `SafeBlock.useItemOn`, `lib/net/Codecs` and
      `lib/ui/GuiHelpers` (never used), the dead `createClientTooltipComponent` methods, and the
      unused tag constants `ID_CARD_ITEMS`, `DEBIT_CARD_ITEMS` and `WALLET_SLOT_ITEM` (the tags
      themselves are used, through `wallet_items` and Curios).
- [ ] The safe's title as a translation key.
- [ ] Strip the remaining tutorial comments ("works whether SAFE_BLOCK is a BlockEntry or a
      RegistryObject", "THIS YOU HAVE TO DEFINE!", "your existing Codec").

## The npcinteractions merge, 18 Oct

The trader overhaul in progress was found in a second checkout
(`C:/Users/chaos/IdeaProjects/CreateTradeAndOpenMarkets`): the branch
`codex/implement-customizable-shop-tab-system` plus an uncommitted refactor on top of it. Both were
snapshotted without touching that checkout, ported to the 17 Oct names, and merged onto
`feature/npc-interactions` (merge commit `95d0539`).

Fixed while merging:
- **Every villager interaction threw on a dedicated server.** `VillagerInteractionMenu`'s server
  constructor looked the villager up through `Minecraft.getInstance()`. It uses
  `Level.getEntity` now. `VillagerInteractionGameTests` covers it and failed with the server's
  own error first.
- The screen asked for `screen.erranttrade.npcinteractions.wallet_*` translations, while the lang
  file defined `screen.erranttrade.shop.wallet_*`. The lang keys follow the code.
- `WalletFinder` reached Curios directly; it goes through `CuriosCompat`.
- An IDE rename had turned "render" into "renderPanel" inside comments in both renderers.

Done on 18 Oct, after the merge (`docs/design/trading.md` is the record):
- [x] **Trading works.** Buying and selling through the menu, paid from loose coins then the
      wallet, income to the wallet; vanilla villagers trade their own offers at 100 copper bits
      per emerald; every trade gives villager experience by its coin value. The vanilla takeover
      stays on: villagers trade through the new menu.
- [x] The client reads the server's tabs and never resolves its own; the double-written tab type
      that would have corrupted them is fixed.
- [x] The menu closes on distance, and the villager is claimed while it is open.
- [x] The takeover leaves name tags, leads, spawn eggs, sneaking, babies and sleeping villagers to
      vanilla.
- [x] Stock is saved on the villager and survives restarts; the static in-memory cache is gone.

Still open (details in [trading §8](design/trading.md#8-not-yet)):
- [x] Reputation and Hero of the Village discounts apply to converted offers, as in vanilla.
- [x] At most 11 rows showed, over the inventory: the shop is a paged 3×9 grid now.
- [ ] Buying several at once.
- [ ] **Tabs mix drawing into data.** Split each tab into its data (common) and a renderer
      (client).
- [ ] **`docs/ShopTabs.md` describes an older shape**: `ShopTabType`, `ShopTabRegistry`,
      `ShopDefinitionLoader`, `data/<ns>/shops/`, all renamed since.
- [ ] Prices, stock rolls and buybacks could move into the domain, as payment and experience did.
- [x] **Stock ranges were ignored** (found 18 Oct by the economy work's logs). A shop's
      `{"min_inclusive": 2, "max_inclusive": 6}` went to `IntProvider.CODEC`, which wants a
      `"type"`, so it failed with a warning and the entry's stock was unlimited: the farmer's
      second bread line and the banker's gold marks never sold out. Both tab types read the
      shorthand as a uniform range now. `TradingGameTests.aStockRangeRollsWithinItsBounds`.
- [ ] The two tab types (`BuyShopTabType`, `SellShopTabType`) duplicate their stock parsing
      word for word; one shared reader (or the domain) would have needed the fix once.
- [ ] The sell tab's "villager can't afford" colouring compares the till with the price only; in
      a village the fee is due as well, so a till just short by the fee shows white and the
      server refuses. The server is right; the screen could be told the fee.
- [ ] The second checkout still holds its uncommitted changes and a branch
      `wip/npcinteractions-snapshot` created for the merge. Retire that checkout so the two cannot
      diverge again.
