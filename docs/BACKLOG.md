# Backlog: testing, bugs, cleanup

Opened 17 Oct 2026 from a first read of the whole codebase. Line numbers are as of commit
`5acedc7`. Each bug was found by reading the code, not by running it. Confirm each one with a
test first, then fix it.

## Bugs

Ordered by severity. **Coin loss or duplication comes first.**

### Coin conservation

1. **Sneak double-tap on a coin block destroys the coins it deposits.**
   [CoinBlock.java:342](../src/main/java/com/errantmc/markets/block/CoinBlock.java#L342)
   `dumpAllCoinsFromInventory` adds every stack to the same original `c`, never the running total,
   so each `setCurrency` overwrites the last. Line 367 then writes the original `c` back. The
   player's coins are shrunk and the pile ends up unchanged.
2. **"Deposit all" in the wallet moves the wrong slots.**
   [WalletMenu.java:213](../src/main/java/com/errantmc/markets/screen/wallet/WalletMenu.java#L213)
   For non-coin wallet items it calls `quickMoveStack(player, i)` with an *inventory* index, but
   `quickMoveStack` takes a *menu slot* index. Inventory 0–8 are wallet slots 0–8 in this menu,
   so the hotbar sweep moves wallet contents out. The offhand loop at line 226 passes offhand
   index 0, which is wallet slot 0.
3. **Coins over `Integer.MAX_VALUE` vanish.** `CurrencyData.add` clamps and reports `remaining`,
   but `WalletBalance.add`, `WalletAutoDeposit` (which discards the item entity anyway),
   `WalletMenu.depositAllCoinsFromPlayer` and `autoDepositFromSlots` ignore `remaining`.
   `CurrencyData.add(CurrencyData)` and the 8-int `add` overflow without clamping.
4. **`CurrencyData.totalCount()` overflows.** It sums `int`s before the result widens to `long`.
   The same pattern appears in `CoinItem.appendHoverText` (`int total = count * value`).
5. **Placing a coin overwrites whatever is above the clicked block.**
   [CoinItem.java:57](../src/main/java/com/errantmc/markets/item/CoinItem.java#L57)
   `useOn` always targets `pos.above()`, whatever face was clicked, and calls `setBlock` without
   checking `canBeReplaced` or `canSurvive`. Clicking the side of a block that has something on
   top replaces that thing with a coin pile.
6. **The coin stack item can place a pile where it cannot survive.**
   [CoinStackItem.java:46](../src/main/java/com/errantmc/markets/item/CoinStackItem.java#L46)
   It checks `canBeReplaced` but not `canSurvive`, so a pile placed against a wall or on another
   pile sits unsupported until the next neighbour update drops its whole balance. Also,
   `ctx.getPlayer()` can be null (dispensers, fake players).
7. **`SafeBlockEntity.clearContents` clears slot 0 twenty-five times.**
   [SafeBlockEntity.java:41](../src/main/java/com/errantmc/markets/block/entity/SafeBlockEntity.java#L41)
   Not called anywhere yet, which is why it hasn't mattered.

### Robustness

8. **Curios classes are linked from a class that always loads.** `WalletAutoDeposit` references
   `CuriosApi` inline behind `isLoaded`. That usually works because the JVM resolves lazily, but
   it is fragile. Move it to a `CuriosCompat` class that is only touched after the check.
9. **`WalletAutoDeposit` scans for item entities around every player every tick.** It also
   ignores `ItemEntity` ownership (`target`/thrower) and other mods' pickup events. Consider
   `ItemEntityPickupEvent.Pre` instead of a tick scan.
10. **`CoinBlock.LAST_SNEAK_USE` is a static map that never shrinks.** Small, but it grows by one
   entry per player per server lifetime.
11. **`CoinBlock` logs at INFO on every interaction** (`useItemOn`, `useWithoutItem`), and
    `WalletMenu.clickMenuButton` logs every button press.

## Testing

### Phase 1: test infrastructure
- [ ] Add a `src/test/java` source set and JUnit 5 (`testImplementation` + `useJUnitPlatform()`).
- [ ] Decide whether unit tests run with `neoForge.unitTest { enable() }`, which boots FML, or
      stay pure. Prefer pure; see the refactor item below.
- [ ] Add a gametest holder class and one trivial gametest so `runGameTestServer` passes. Add the
      empty structure template it needs.
- [ ] Add `runGameTestServer` to CI after `build`.

### Phase 2: characterisation tests (pin current behaviour, bugs included)
- [ ] `CurrencyData`: add, remove, clamping, `totalValue`, `totalCount`, `getHighestDenomonation`,
      codec round-trip (NBT) and stream codec round-trip.
- [ ] `CoinDenomination` ↔ coin item mapping covers every denomination (registry-list test).
- [ ] `CoinStackItem.tier` thresholds.
- [ ] Gametests: coin block place, insert, shift-insert, double-tap deposit (bug 1), left-click
      withdraw, break with ≤20 stacks, break with >20 stacks (one coin stack item), break the
      support block.
- [ ] Gametests: wallet auto-deposit; wallet withdraw button; deposit-all (bug 2).
- [ ] Gametest: safe drops its contents when broken.
- [ ] Coverage tests: every registered item has a lang key and an item model; every item is in
      the creative tab.

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
