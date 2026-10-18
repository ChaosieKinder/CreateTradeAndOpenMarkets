# Trading with villagers

The mod replaces vanilla's trading screen with its own: right-clicking a villager opens the
interaction menu, whose tabs come from a data-pack shop definition
(`data/<namespace>/npcinteractions/<profession>.json`) or, for a profession with none, from the
villager's vanilla offers. Prices are money, counted in copper bits; nothing costs emeralds.

This document records how it works and what was decided. Code that disagrees with it is a bug,
or a decision to write down here.

## 1. Who decides what

**The server decides everything; the client only shows it.** The server resolves the tabs, sends
them in the open packet, and sends them again after every trade (`InteractionTabsSyncMessage`).
A click on an entry is a menu button (`VillagerInteractionMenu.tradeButton`), and the server
checks and performs the trade (`ShopTrades`). The client never works out tabs, stock or prices.

Why: a client that rolls its own stock can disagree with the server about what is for sale, and
a client that decides what a trade costs is a dupe exploit.

## 2. When the menu opens

Only where vanilla would open its trading screen (`VillagerInteractionEvents.opensTrading`). Name
tags, leads, spawn eggs, sneaking, babies, sleeping villagers, one already trading, the
unemployed and nitwits all stay vanilla's.

While the menu is open the villager is claimed (`setTradingPlayer`), as vanilla does: it stands
still and no one else can trade with it. The menu closes when the villager dies or the player is
more than eight blocks away.

## 3. Paying and being paid

*Decided by the author, 18 Oct.*

- **Paying takes loose coins first, then the wallet.** Within each, the smallest coins go first,
  so buying clears small change rather than breaking large coins (`Payment.plan`, in the domain).
- **Change and sale income go into the wallet if one is carried**, as far as it fits, and
  otherwise into the inventory as coins, dropped at the player's feet if it is full
  (`PlayerMoney.give`). Change is made in the fewest coins (`Payment.change`).
- **A refused trade changes nothing.** Stock, extra items and money are all checked before
  anything moves, and the player is told why.

## 4. Vanilla villagers

*Decided by the author, 18 Oct.*

A villager with no shop definition trades its own vanilla offers, converted:

- **One emerald is worth one silver crown**, 100 copper bits (`trading.emeraldValue`), applied to
  the offer's current costs, after demand.
- **A cost that is not emeralds stays an item** the player must also hand over (the librarian's
  book), matched by the offer's own `ItemCost`. An offer with two such costs is skipped.
- **Each entry stays linked to its offer.** Stock is the offer's remaining uses, a trade uses it
  up, and the villager restocks at its workstation as in vanilla.

## 5. Progression

*Decided by the author, 18 Oct: every trade drives progression, scaled by its coin value rather
than "one trade = one trade", because these shops can be much larger than vanilla's.*

- **Villager experience is the trade's worth** at `trading.bitsPerVillagerXp` copper bits per
  point (50: a one-emerald trade is 2 points, as most novice trades are). What does not make a
  whole point is kept on the villager for its next trade (`TradeXp`, in the domain), so many small
  trades add up to the same as one large one.
- **It reaches the villager through `notifyTrade`**, as a vanilla trade does: the villager gains
  experience, levels up when the screen closes, and remembers the player favourably.
- **The player gets the same amount** as experience orbs, and not vanilla's fixed orbs per trade.
  Those are switched off in the reporting offer, which has to be built through the codec because
  no public constructor can turn them off. Fixed orbs per trade would let anyone farm experience
  by buying something cheap a thousand times.

## 6. Stock over time

A defined shop's stock is rolled per villager per day. The tabs are rebuilt on every resolve from
the definition and a random seeded by the day and the villager, so they come out the same all
day, and what has been bought since is the villager's `ShopStock`, saved with the villager. A new
day restocks. Nothing is cached in memory.

## 7. The screen

Both kinds of tab show their entries the same way (author's decision, 18 Oct): one entry per cell
of a 3×9 grid drawn from `tab_sell.png` into `shop_base`'s brown panel, 27 to a page, paged with
the arrows under the grid or the scroll wheel. Hovering a cell shows the item's own tooltip, then
the price (or what it pays), anything else the trade needs, the stock, and whether the player can
make the trade now. An entry out of stock is greyed out. `tab_buy.png`, a 3×4 grid either side of
an arrow, is no longer used.

The grid's geometry (`SimpleShopTab`) is shared by drawing, clicking and tooltips, and a gametest
pins it to the art.

## 8. Not yet

- **Reputation and Hero of the Village discounts do not apply.** Vanilla sets them in
  `Villager.updateSpecialPrices` when trading starts, a private method this menu does not call,
  so converted prices are always the undiscounted ones.
- **One click is one trade.** There is no buying several at once.
- **Tabs mix drawing into data**: `InteractionTab` and its subclasses carry client drawing code in
  objects the server also builds. Split each into its data and a client renderer.
- `docs/ShopTabs.md` describes an older shape of the tab framework.
