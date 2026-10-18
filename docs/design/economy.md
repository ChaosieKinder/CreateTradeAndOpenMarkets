# The economy: NPC wallets and where money comes from

**Status: decided 18 Oct; step 1 (NPC balances) built 18 Oct, steps 2–6 not yet.** §1–§5 describe how shops work today, the problem NPC
wallets solve, and the options; §6 records what the author chose, and §7 the model that follows
from it. Build order in §8.

## 1. How shops are built today

A shop is resolved per villager whenever its menu opens or a trade completes
(`InteractionTabManager.resolveTabs`):

1. **A definition for the profession**, `data/<ns>/npcinteractions/<profession>.json`: an ordered
   list of tabs, each `{ "type", "title", "data" }`. The built-in types are `erranttrade:buy` and
   `erranttrade:sell`, whose `data.entries` are `{ item, price, stock }`. `price` is copper bits;
   `stock` is a number, an `IntProvider` range, or weighted `options`. A sell tab may set
   `auto_buybacks` to re-buy what the player sold, at a percentage of the buy price.
2. **Rolled per villager per day.** A random seeded by the world, the day and the villager's UUID
   rolls each entry's stock, so every farmer has the same goods but its own stock. What has been
   bought since is the villager's `ShopStock`.
3. **Or the vanilla offers**, converted at 100 copper bits an emerald, when the profession has no
   definition.

What this cannot express yet:

- **Every villager of a profession sells the same list**, whatever its level. A master farmer and
  a novice offer the same goods; only stock differs.
- **Prices are fixed.** Nothing moves them: not supply, not demand, not reputation (for defined
  shops), not how much money is around.
- **A villager buys while its stock lasts**, with money it does not have. *(Fixed by step 1: a
  villager now pays from its till, `VillagerBalance`, and the farmer's sell tab has no stock caps.)*

## 2. Where money comes from today

**The only source of new money is a villager paying a player.** No recipe makes coins from
anything but coins, no loot drops them, and the banker only exchanges coins for coins. So:

- **Every sale to a villager creates money from nothing**, and every purchase from one destroys
  it.
- The amount of money in the world is just the difference between the two. Nothing limits it, and
  a player who farms wheat for the farmer's sell tab prints coins.

That is the hole NPC wallets close: if a villager can only pay with money it has, money is
conserved, and the questions become *where new money enters* and *where it leaves*. Those are the
economy's design.

## 3. The proposal: every NPC has a balance

Each trading NPC carries a **balance** (saved on the entity, like `ShopStock`):

- **Selling to the player puts the price into the NPC's balance**, instead of destroying it.
- **Buying from the player takes the price from the balance**, and is refused ("the farmer can't
  afford that") when it runs out. This replaces the sell tab's fixed daily stock: a villager keeps
  buying wheat while it has money (author's example), and an optional cap still limits how much it
  wants. Stock on buy tabs stays as it is.
- **The balance is visible in the shop**, as the player's wallet is, so the player can see the
  villager is short before trying.

*Decide:* **coins or a balance?** A balance in copper bits (recommended) can always make change,
so the till never refuses a trade for want of the right coins; it is what a shopkeeper's
till is. Coins would be more physical, but a villager could then hold 100 bits as a gold mark
and be unable to pay 20.

## 4. Where new money enters and leaves

This decides whether the economy inflates, deflates or holds steady. Options, which combine:

| Source | How | Effect |
|---|---|---|
| **Wages** | Each NPC gains a daily income by profession and level, up to a ceiling | steady money supply; villagers recover from being bought out |
| **A mint** | A block that strikes coins from metal: copper, iron/silver, gold, emerald | money is backed by something the player mines; the supply follows the world's ore |
| **Starting balance** | An NPC spawns with a balance by profession and level | a one-off endowment; alone, the world runs dry |
| **The banker** | The bank holds the village's reserve and lends or pays out of it | a central bank; needs bank accounts (README, planned) |

| Sink | How |
|---|---|
| **Upkeep** | NPCs spend part of their balance daily (on "supplies"), above a floor |
| **Restocking** | A villager pays to restock its buy tabs, so selling to players costs it something |
| **Melting** | The mint's reverse: coins back into metal |
| **Fees and taxes** | A cut of each trade to the village (see §5) |

Recommended start: **wages plus restocking.** Each NPC earns a daily wage and spends to restock.
That holds the supply roughly steady without needing a new block. The mint is the natural next
step, because it ties coins to ore, and it reads as this mod's own (README: "Custom Currency
Items").

## 5. Larger systems this opens

In rough order of how much each depends on the ones before:

1. **Prices that move.** With a balance and stock, prices can follow them: a villager short of
   money pays less for what it buys; one short of stock charges more for what it sells. That is
   supply and demand without a market simulation, and it all lives in the domain as one pricing
   function, tested without a world.
2. **Defined shops by level and variety.** Entries gain `min_level` (masters sell more), and a tab
   can draw *N* of a weighted pool per villager, so two farmers differ.
3. **A village treasury.** Villagers of one village (by meeting point) pay a share of their income
   into a shared balance; the banker manages it, and wages come out of it. Villages become
   economies of their own, richer or poorer.
4. **Bank accounts** (README, planned): the player's balance with the banker, deposits and
   withdrawals, paper notes backed by it.
5. **Measuring it.** A command that reports the money supply (players' coins and wallets, NPC
   balances, treasuries) and its change per day. Without it nobody can tell whether a change
   inflates the economy; with it, the README's "inflation detection" becomes possible.

## 6. Decisions

*All by the author, 18 Oct.*

- **An NPC holds a balance in copper bits**, not coins: a till that can always make change.
- **New money comes from daily wages and starting balances.**
- **Money leaves through restocking costs, daily upkeep, and fees or taxes.**
- **Villages pool money in a treasury from the start, managed by the banker.**
- **The treasury pays the wages.** New money enters in one place, the treasury's daily income,
  which makes the money supply something that can be measured and controlled.

## 7. The model

```
  new money ──► TREASURY  (starting balance, then daily income by population)
                 │   ▲  ▲
         wages   │   │  └── upkeep (daily, from each villager above a floor)
                 ▼   │
             VILLAGER ── a fee on every trade ──► TREASURY
             balance
               │  ▲
  pays player  │  │  player pays
  for goods    ▼  │  for goods
              PLAYER

  restocking:  villager ──► destroyed
```

- **A village is its meeting point**: the bell a villager remembers (`MEETING_POINT`). Its treasury
  is world state (`SavedData`, AGENTS §6), keyed by that position. A villager with no meeting point
  belongs to no village: it keeps its own balance, earns no wage and pays no tax.
- **The banker manages it**: the banker's shop shows the treasury, and later bank features
  (accounts, notes, loans) work against it.
- **Nothing ticks.** Daily income, wages and upkeep are settled lazily, for the days elapsed, when
  a villager or treasury is next touched (a trade, a shop opening), and capped to a few days so a
  forgotten village does not settle a year at once. The same rule as metalworks' lazy heat.
- **Wages are paid only from what the treasury has.** A poor village pays short wages; its
  villagers buy less from players. That is the feedback the economy needs.
- **Every number is config**, by profession and level where it matters: starting balances, the
  treasury's income per villager, wages, the upkeep rate and floor, the trade fee, restocking
  costs. Defaults chosen so a vanilla-sized village is roughly stable, and tuned with §8.3.
- **Conservation is testable.** Apart from the treasury's income and the restocking cost, every
  movement is a transfer, so a test can sum all money (players, villagers, treasuries) before and
  after any trade, wage or upkeep and require it to be unchanged.

## 8. Order

1. **NPC balance.** An attachment, with its starting balance; shown in the shop. Selling to a player
   pays into it, buying from one pays out of it, refused when it runs dry; the sell tab's daily
   stock becomes an optional cap. Conservation tests. **Built 18 Oct:** the `erranttrade:balance`
   attachment, set lazily to `startingBalance` + `startingBalancePerLevel` × (level − 1) (config,
   1000 and 1000 bits); applies to vanilla-converted shops as well as defined ones. The till shows
   in the shop's header and in the sell tooltips; `EconomyGameTests` covers conservation, refusal
   and buying until the till runs dry.
2. **The treasury.** Villages by meeting point, the treasury as SavedData, trade fees into it.
3. **The daily settlement.** Treasury income, wages, upkeep and restocking, lazily, with their
   config.
4. **The money-supply command**, so every later change can be measured and the defaults tuned.
5. **Dynamic prices**, as a domain function of stock and balance.
6. **Then** shop variety by level, bank accounts and notes, and the banker's view of the treasury.
