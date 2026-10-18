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

## Refactoring and cleanup

Do these after their area has tests.

- [ ] **One source of truth for denominations.** Give each `CoinDenomination` a way to get its
      item (or have `CoinItem` own the mapping by `denom()`), and replace the eight-way
      `if`/`switch` blocks in `CoinItem.getDenomination`, `createCurrencyItem`, `isCoinItem`,
      `CoinBlock.onRemove` and `CurrencyData`. Consider storing `CurrencyData` as a map or array
      keyed by denomination. **The codec field names are a save format** (AGENTS §5), so keep
      them or migrate.
- [ ] Split `CurrencyData` into a pure value type (no `ItemStack`, no `CoinItem`) plus an adapter,
      so it can be unit-tested without FML.
- [ ] One "deposit coins into a balance" routine shared by the coin block, the wallet menu and
      auto-deposit. They currently do it four different ways, which is how bugs 1–3 crept in.
- [ ] Replace `CoinBlockEntity`'s `System.err` with the logger; use `resultOrPartial(LOG::error)`.
- [ ] Remove the MDK example config entries (`logDirtBlock`, `magicNumber` …) and the unused
      `validateItemName`. **This changes the config file**, so mention it in the changelog.
- [ ] Remove dead code: `TradeComponents.register(String, …)` (unused), commented-out blocks in
      the mod constructor, `if(false && …)` in `SafeBlock.useItemOn`, the unused `org.jline` import.
- [ ] Locale-independent money formatting (AGENTS §6).
- [ ] Literal strings to translation keys: coin stack tooltip, safe title.
- [ ] Fix the spelling of `getHighestDenomonation` / `denomonation` (public API, so rename in one change).
- [ ] Consider datagen for recipes, tags, lang and models once the coverage tests exist. It is
      already wired in `build.gradle`.
- [ ] Decide the future of `com.errantmc.markets.lib` (formerly `errantlib`): keep it in-tree, or extract it as a shared library with
      errant-metalworks' `errant-core`. Ask the author.
