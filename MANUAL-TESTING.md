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

On `feature/npc-interactions` (the trader overhaul, in progress):

- [ ] **Right-clicking a villager** opens the mod's interaction screen, titled with the villager's
      name, showing tabs and the wallet balance (or *Wallet not found*). The player's inventory
      and hotbar items sit in the background's slots (they were 50 px off before 18 Oct).
- [ ] **The grid.** Buy and sell tabs both show a 3×9 grid of items in the brown panel, each item
      in its cell. Hovering one shows its tooltip, then price (or what it pays), stock, anything
      else it needs, and whether you can afford it / how many you have. Out-of-stock items are
      greyed. A shop of more than 27 (try a librarian's book trades, or a big JSON shop) shows
      *Page 1 of 2* with arrows, and the scroll wheel pages too.
- [ ] **Buying.** Click a row: the item arrives, loose coins go first, then the wallet, and the
      change lands in the wallet. Stock drops by one. Without the money, or out of stock, the
      action bar says why and nothing changes.
- [ ] **Selling.** Click a sell row with the goods in your inventory: they go, and the money goes
      into the wallet (or into the inventory as coins without one).
- [ ] **The till.** The header shows *Till: $X* between the villager's name and your wallet. Buying
      raises it by the price, selling lowers it. Keep selling wheat to a farmer: it buys until the
      till is short, then the sell rows turn red and the action bar says it can't afford that.
- [ ] **The treasury.** In a village with a bell, a banker (a safe as its workstation) shows
      *Treasury: $100.00* at first. Trading with the village's farmer raises it by 5% of each
      price. Buying a gold mark from the banker for $5.00 leaves the treasury where it was. Restart
      the world: the treasury keeps its balance.
- [ ] **A day later** (`/time add 24000`), reopening the farmer's shop shows its till up by its
      wage (less restocking for what it sold you yesterday), and the banker's treasury moves too.
      `/time add 240000` pays only three days.
- [ ] **A vanilla villager** (say a toolsmith) shows its own offers priced at $1.00 per emerald. A
      librarian's book trade asks for the book as well. Using an offer up shows stock 0; the
      villager restocks at its workstation.
- [ ] **Progression.** Trading enough levels the villager up when the screen closes, and new
      trades appear. Experience orbs match the trade's worth, not one burst per click.
- [ ] **Name tags and leads** still work on villagers, babies shake their heads, and walking away
      closes the shop.
- [ ] **Restart the world** after buying out an item: it is still bought out that day, and
      restocks the next.
- [ ] **The farmer** shows the tabs from `data/erranttrade/npcinteractions/farmer.json`; a
      profession without a definition shows buy and sell tabs derived from its vanilla offers.
- [ ] **The same tabs** come back on reopening the same day (the stock does not reroll).
- [ ] **On a dedicated server** (`runServer` and a second client): the screen opens, rather than
      throwing as it did before 18 Oct.
- [ ] **A banker** appears when an unemployed villager finds a safe, and opens the banker tabs.

## Findings

None yet.

## Passed

Nothing yet.
