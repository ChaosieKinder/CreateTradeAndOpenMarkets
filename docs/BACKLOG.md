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
- [ ] Unit: `CurrencyData` codec and stream-codec round trips, pinning the field names (a save
      format), `getHighestDenomonation`, and `CoinStackItem.tier` thresholds.
- [ ] A banker villager actually claiming a safe. That needs a villager and many ticks, so it is
      slow and possibly flaky. The POI wiring is covered.
- [ ] Client-only behaviour (the shop screen replacing the merchant UI, tooltips, renderers) has
      no automated coverage, because gametests run on a dedicated server.

### Noticed while writing them (not bugs, but worth a decision)
- The README says right-click deposits coins and "shift for a whole stack", but a plain
  right-click has always deposited the whole held stack. The test pins what the code does.

## Audit, 17 Oct (second pass)

A second read of the whole codebase after the gameplay tests went in. Findings are ordered by
severity. Each **Open** item is either pinned by a test (named) or argued from the code.

### Open bugs

| # | Bug | Evidence | Suggested fix |
|---|---|---|---|
| 16 | **Wallet duplication.** The open wallet sits in one of the menu's own hotbar slots, and the grid is written back on close only if the wallet is still in hand. Take an item out of the grid, throw or move the wallet out of its slot, and the wallet keeps its old contents: the item now exists twice. The opposite order loses items | pinned: `WalletGameTests.throwingTheOpenWalletDoesNotDuplicateItsGrid` (one card becomes two) | lock the open wallet's slot (a `Slot` whose `mayPickup` is false for that stack, as bundle and backpack mods do), and write the grid through on every change instead of on close |
| 17 | **The identity card sync broadcasts every player's location.** Every 60 s (and on each login) every client receives every online player's position, dimension, respawn point, health, armour, XP and playtime, whether or not it holds anyone's card. Any client, modded or not, can read it | `IdentityCardDataSyncHandler.broadcastToAll`, `IdentityCardPlayerSnapshot` | send a snapshot only for cards the client is actually showing (on request when a card is rendered or opened, or for the holder's own card), and decide which fields a card may show at all. **Product decision** |
| 15 | **A broken safe is lost.** It sets `requiresCorrectToolForDrops`, but is in no `mineable/*` tag, so no tool is correct and it never drops itself (its contents do). Its destroy time of 1200 also makes it near-unbreakable | pinned: `SafeGameTests.aPickaxeIsTheRightToolForASafe` | add it to `mineable/pickaxe` (and `needs_iron_tool` or `needs_diamond_tool`). Whether it should be this hard to break is a **product decision** |
| 18 | **The shop screen talks to a different menu than the server.** It swaps the client's `MerchantScreen` for a `ShopScreen` over a client-only `ShopMenu` with its own 36 slots, but the server still runs vanilla's `MerchantMenu` (3 trade slots, then the inventory), and `player.containerMenu` stays that menu. Clicks are sent with `ShopMenu` slot indices and applied to `MerchantMenu` slots, so clicking the top-left inventory slot acts on the payment slot. The server stays authoritative, so this desyncs rather than dupes. The code is marked WIP | argued from the code: `ErrantTradeClient.onScreenOpening`, `ShopMenu`. Client-only, so no gametest can reach it | make `ShopScreen` an `AbstractContainerScreen<MerchantMenu>` over the real menu, and only redraw it. Drop `ShopMenu` and the unused `SHOP_MENU` registration, which crashes if ever opened (`merchantMenu` is null there) |
| 19 | **The wallet grid accepts any item**, shulker boxes and other containers included: unlimited nested storage, and a classic route to oversized-NBT packets. The README describes it as storage for "security cards, cashiers checks, etc.", and the tags `id_card_items` and `debit_items` exist for that but are unused | `WalletMenu` slots' `mayPlace` refuses only wallets | accept only tagged items (coins, coin stacks, ID cards, debit cards). **Product decision** |

### Smaller defects

- `ShopScreen` declares its own `leftPos`/`topPos`, shadowing `AbstractContainerScreen`'s, so the
  background and the slots are positioned from different origins.
- `InventoryHelper` mixes slot constants used by common menus with client-only drawing
  (`GuiGraphics`, `RenderSystem`). It works because the constants are inlined at compile time;
  the first non-constant use from a menu breaks the dedicated server. Split it.
- `IdentityCardClientCache` is never cleared, so data from one server carries over to the next.
- `IdentityCardItem` imports client classes (`BlockEntityWithoutLevelRenderer`,
  `IClientItemExtensions`) that it does not use.
- `WalletTooltip.createClientTooltipComponent` and `IdentityCardTooltip.createClientTooltipComponent`
  are dead (the factories are registered in `ErrantTradeClient`) and name client types from common
  classes.
- `CoinDenomination.format` holds legacy `§` colour codes. Use `ChatFormatting`/`TextColor`.
- Player-facing literals: "Buy"/"Sell" (shop), "Safe" (safe title), the coin stack's "Coins:" and
  "Value:".
- The coin pile renderer recomputes its whole layout, and allocates `ItemStack`s, for every pile
  every frame. The layout depends only on the balance, so it can be cached per balance.

### Structure: a pure `-domain` module

**Recommended.** Split the logic that has no business knowing about Minecraft into a plain
`java-library` subproject, `errant-trade-domain`, which declares no Minecraft dependency. The
compiler then enforces the boundary. This is the shape errant-metalworks uses (ARCHITECTURE §4.2
there).

What moves (about 400 lines today):

- **Denominations and value**: `CoinDenomination` minus its colour, and `CurrencyData`'s arithmetic
  as a `Purse` value type (counts per denomination, `canAdd`, `add`, `remove`, totals) with no
  `ItemStack`, no codecs and no registry access. The mod keeps a thin adapter: the codecs (which
  are the save format, so the field names stay), the data component and `ItemStack` access.
- **The deposit rule**: what `WalletMenu.absorb`, the pile and auto-deposit each do ("take what
  fits, report the rest") as one function over counts. Bugs 1–3 came from four copies of it.
- **Money formatting**, locale-independent, in one place.
- **The coin pile layout** (`ringSlots`, `getStacks`, `thickenFullStacks`): pure geometry, today
  untestable inside a renderer.
- **Everything on the roadmap that is economics**: change-making, bank balances, note values,
  trader pricing, inflation detection. These are the parts most worth testing, and they start
  life in the domain.

Why it is worth it now:

- **Fast tests.** Domain tests are plain JUnit in milliseconds. Today every unit test boots FML
  (`neoForge.unitTest`), about 10 s before the first assertion.
- **The boundary is enforced, not hoped for.** A `-domain` project that cannot see Minecraft
  cannot grow an `ItemStack` dependency.
- **It is cheap now and expensive later.** Most upcoming features are domain-heavy.

Cost: a multi-project build (`settings.gradle` include, the mod depending on the domain and
shipping it inside its jar with Jar-in-Jar or by merging the classes), and keeping the domain's
package unique (`com.errantmc.markets.domain`; AGENTS §3).

**Not recommended: a library shared with errant-metalworks.** It is on Minecraft 26.2 and this
mod is on 1.21.1, so no Minecraft-facing code can be shared, and the two mods have no domain logic
in common. What is shared is ideas: the gametest player, the registry-list tests, the empty
template tool. Copy those. Revisit this if the two mods ever share a Minecraft version, or if an
economy API for other mods to call becomes a goal (that would be its own `-api` artifact).

### Build and tests

- **Gametests ship to players.** They live in `src/main/java` with their structure template, so
  every release jar carries them. Move them to a `gametest` source set that is on the gametest
  run's classpath and not in the jar.
- **Datagen for what is derivable.** The 18 coin recipes follow from `CoinDenomination` values,
  so generate them and the value test becomes a check on the generator. Lang, item models and
  tags are small but would stop drifting. The `src/generated/resources` wiring already exists.
- **Registry IDs, while worlds are already broken.** The rename already invalidated every saved
  world, so this is the cheapest moment to tidy IDs: `safe_block` to `safe`, `coin_block` to
  `coin_pile`, `wallet_leather` to `leather_wallet`, and `safe_blockentity` and `coin_blockentity`
  to `safe` and `coin_pile`. After the first public release each rename needs a migration.
  **Decide before release.**

## Refactoring and cleanup

Do these after their area has tests. Most of the first three come for free with the domain split.

- [ ] **One source of truth for denominations.** Give each `CoinDenomination` a way to get its
      item (or have `CoinItem` own the mapping by `denom()`), and replace the eight-way
      `if`/`switch` blocks in `CoinItem.getDenomination`, `createCurrencyItem`, `isCoinItem`,
      `CoinBlock.onRemove`, `CoinBlockEntityRenderer.getStacks`, `WalletScreen`'s button ids and
      `CurrencyData`. Consider storing `CurrencyData` as a map or array keyed by denomination.
      **The codec field names are a save format** (AGENTS §5), so keep them or migrate.
- [ ] Split `CurrencyData` into a pure value type (no `ItemStack`, no `CoinItem`) plus an adapter,
      so it can be unit-tested without FML.
- [ ] One "deposit coins into a balance" routine shared by the coin block, the wallet menu and
      auto-deposit (the wallet menu's `absorb` is the start of it).
- [ ] Replace `CoinBlockEntity`'s `System.err`, and the `System.out` in `SafeMenu` and `ShopMenu`,
      with the logger.
- [ ] Remove the MDK example config entries (`logDirtBlock`, `magicNumber` …) and the unused
      `validateItemName`. **This changes the config file**, so mention it in the changelog.
- [ ] Remove dead code: `TradeComponents.register(String, …)`, commented-out blocks in the mod
      constructor, `if(false && …)` in `SafeBlock.useItemOn`, `lib/net/Codecs` and
      `lib/ui/GuiHelpers` (never used), the dead `createClientTooltipComponent` methods, and the
      unused tags `CURRENCY_ITEMS` and `WALLET_SLOT_ITEM` (or put them to work, as #19 suggests).
- [ ] Locale-independent money formatting (AGENTS §6).
- [ ] Literal strings to translation keys (see Smaller defects).
- [ ] Fix the spelling of `getHighestDenomonation` / `denomonation` (public API, so rename in one change).
- [ ] Strip the remaining tutorial comments ("your logger here", "works whether SAFE_BLOCK is a
      BlockEntry or a RegistryObject", "THIS YOU HAVE TO DEFINE!").
