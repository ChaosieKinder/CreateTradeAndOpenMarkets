# Awaiting manual testing

What the gametests cannot reach: screens, tooltips, rendering, and how things feel in the hand.
Everything below was built and covered headless on 17 Oct and has not yet been seen in a client.
The detail behind each item is in [docs/BACKLOG.md](docs/BACKLOG.md).

Run with `.\gradlew.bat runClient` (JEI and Curios load with it). **Do not build while it is open.**
Use a new world: nothing saved before 17 Oct loads.

**Layout.** [Walk next](#walk-next) is the current list, in the order a session would take it.
Findings go in [Findings](#findings), raw. [Passed](#passed) is what has been signed off.

## Walk next

### 1. Names and art (after the rename)

- [ ] **The creative tab** is called *Errant Trade and Markets* and holds the eight coins, the
      Leather Wallet, the Identification Card and the Safe, each with its name and icon. No
      `item.erranttrade.…` keys, no purple-black checkerboard.
- [ ] **The coin stack** (break a pile of more than 20 stacks' worth) is named *Coin Stack*, its
      tooltip reads *Coins: …* and *Value: $…*, and its icon grows as the pile's worth does.
- [ ] **Money reads `$1,234.56`** in coin tooltips, the wallet tooltip and the wallet screen,
      coloured by the most valuable coin.

### 2. Coin piles

- [ ] **Placing.** A coin on top of a block starts a pile of one. On the side of a block, nothing
      happens and the coin stays in hand (it used to land on top of the clicked block). A coin
      never replaces a block.
- [ ] **Depositing.** Right-click a pile with coins: the whole held stack goes in. With a coin
      stack: it merges whole. Sneak, then sneak-click again quickly with an empty hand: every
      coin in the inventory goes in, offhand included, and none vanish.
- [ ] **Taking out.** Left-click takes one coin of the most valuable kind; sneak-left-click up to
      a stack. The last coin removes the pile. The pile never starts to break.
- [ ] **Looks.** A pile's columns sit inside the block, the most valuable in the centre; a big
      pile thickens its columns rather than overflowing. Breaking a pile shows gold flecks, not
      missing-texture particles.
- [ ] **Breaking.** A small pile drops its coins; a big one drops one coin stack. In creative it
      drops nothing. Breaking the block underneath drops the pile.
- [ ] **Frame rate** with a dozen large piles in view: no worse than before.

### 3. The wallet

- [ ] **Opening.** Use it: the screen shows the grid, the eight withdraw buttons with their
      counts, *Deposit All* and the total.
- [ ] **Withdrawing.** Click a coin's button for one, shift-click for up to a stack. A
      denomination you have none of is greyed out. With a full inventory the coins drop at your
      feet.
- [ ] **Depositing.** *Deposit All* takes every coin and coin stack from the inventory and offhand,
      and leaves ID cards where they are. Coins dropped into the grid turn into balance.
- [ ] **The grid takes only wallet things.** ID cards and coins go in; a shulker box, a bundle,
      dirt and another wallet are refused.
- [ ] **The open wallet stays put.** With the screen open, try to pick up, shift-click, drop (Q)
      or number-key-swap the wallet itself, and to offhand-swap it (F). Each is refused. Take
      a card out of the grid, close, reopen: the card is in the inventory, not also in the wallet.
- [ ] **Pickup.** With a wallet anywhere in the inventory, walking over coins puts them in the
      wallet (the pickup sound and animation play). Coins you just dropped yourself wait their
      usual moment first.
- [ ] **Curios.** The wallet goes in the Curios *wallet* slot, and pickup still fills it there.
- [ ] **Tooltip.** Hovering a wallet shows its total and the items in its grid.

### 4. The safe

- [ ] **Placing** faces the player.
- [ ] **Opening** shows its 25 slots, each holding up to 25.
- [ ] **Breaking.** A diamond pickaxe breaks it (slowly, on purpose) and it drops itself and its
      contents. An iron pickaxe gets nothing back. Is the time to break it right?

### 5. Identity cards

- [ ] **Binding.** Sneak-use a blank card: *Stored identity for …*, the card shimmers and shows
      your face and name. Using it again says it is already assigned.
- [ ] **Live data** (level, XP bar, age, spawn) appears on a card you carry, at once after
      binding. On a second client: a card for someone you do not carry shows only the static
      face, name and issue date; carry theirs, and theirs appears within a minute.
- [ ] **In hand, in a frame, in the GUI**: the card renders the right way up in each.

### 6. Villagers

- [ ] **The trading screen** is vanilla's by default, and trading works.
- [ ] With `replaceTradingScreen = true` (Mods → Errant Trade and Markets → Config): the new shop
      background appears with *Buy* and *Sell* tabs, and **clicks act on the slot you click**
      (they did not before). The slots sit where vanilla puts them, so the layout will not match
      the art yet; that is expected until the trader overhaul.
- [ ] **A banker** appears when an unemployed villager finds a safe.

## Findings

None yet.

## Passed

Nothing yet.
