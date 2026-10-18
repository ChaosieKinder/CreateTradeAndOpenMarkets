# What villagers do: services beyond buying and selling

**Status: investigation, 18 Oct.** Nothing here is built. The author's idea: the interaction system
lets each villager carry whatever tabs its profession is given, so a villager can do things, not
only sell them. The librarian enchants your item instead of selling a book; the cartographer goes
exploring and comes back with a map. This document looks at what that needs, what the framework
has already, and what each profession could offer.

## 1. What the framework already has

- **Tab types are registered in code and placed by data.** `InteractionTabRegistry` maps an id
  (`erranttrade:buy`) to an `InteractionTabType`, which builds a tab from a definition's `data`
  object on the server and rebuilds it from the network on the client. A profession's JSON lists
  any tab types in any order. A new service is a new type, and data decides who offers it.
- **The server decides, the client shows** ([trading §1](trading.md#1-who-decides-what)): tabs
  travel in the open packet and after every action, and a click is a menu button the server acts on.
- **Per-villager saved state** (`ShopStock`, `TradeXp`'s remainder) and **lazy settlement by day**
  (stock rolls by day; the economy's wages, [economy §7](economy.md#7-the-model)).
- **Money and progression**: `PlayerMoney`, `Payment`, and experience by a trade's value. Any
  service can charge, and should drive progression the same way.

## 2. What it lacks

Four gaps, in the order the first services hit them:

1. **Actions are hard-coded to buy and sell.** `VillagerInteractionMenu.clickMenuButton` switches
   on `BuyShopTab`/`SellShopTab`. A tab needs its own server-side action handler
   (`handleAction(context, action)`), with the menu only routing to it. *Small, and needed first.*
2. **The menu has no slots of its own.** Enchanting needs a slot to put the item in; a repair, a
   dye or a copy service the same. Menus fix their slots when they are built, so the menu should
   build every tab's slots up front and show only the selected tab's (vanilla's `Slot.isActive`),
   with the selected tab known to the server and anything left in a slot handed back on close.
3. **State that outlives the menu.** An expedition is ordered today and collected in three days, so
   a tab needs saved per-villager state of its own. `ShopStock` generalises to "tab state by tab
   id", and lazy settlement decides what is ready.
4. **Drawing still lives in the data classes** (backlog). Every new tab type makes that worse; a
   tab type should come with a client renderer registered separately.

Also wanted early: **tabs gated by villager level** (a novice librarian sells books, a master
enchants), as `min_level` on any tab or entry.

## 3. Kinds of service

The professions' ideas reduce to a handful of tab types, each one generic and data-driven:

| Type | The player… | The villager… | Examples |
|---|---|---|---|
| **Shop** (built) | buys or sells goods | pays or is paid | everything today |
| **Work** | puts an item in, pays | changes it now | enchant, repair, disenchant, dye, cut stone, cook |
| **Order** | pays, waits days | delivers on a day | the cartographer's map, a bulk smithing job |
| **Contract** | delivers goods over time | pays on completion, or per delivery | the farmer wants 256 wheat this week |
| **Service** | pays | does something for the player | the cleric cures a curse; the banker's accounts |
| **Information** | asks | answers | the cartographer marks a biome; rumours; the village's treasury |

## 4. Profession by profession

| Profession | Today (vanilla) | Could do instead, or as well |
|---|---|---|
| **Librarian** | sells enchanted books | **Work: enchant the player's item** with an enchantment it knows (from its book offers), priced as the book; a master **removes curses** or **disenchants** onto a book |
| **Cartographer** | sells explorer maps | **Order: go exploring** for a structure, back in N days with a completed map; Information: mark the nearest biome |
| **Armourer / Weaponsmith / Toolsmith** | sell gear | **Work: repair** an item for money, without the anvil's experience cost or its "too expensive" ceiling; masters **reforge** (move up a tier with the materials) |
| **Cleric** | sells potion goods | **Service: cure** an effect or zombification; Work: brew a potion from ingredients |
| **Mason** | sells stone | **Work: cut stone in bulk** (stonecutter recipes) |
| **Butcher / Fisherman** | buy and sell food | **Work: cook** raw food; Contract: a catch or a cut to order |
| **Farmer** | buys crops | **Contract: deliveries** at a price above its shop's |
| **Leatherworker / Shepherd** | sell leather and wool | **Work: dye** armour or wool; shear-and-dye in bulk |
| **Fletcher** | sells arrows | **Work: tip arrows** with a potion |
| **Banker** (ours) | exchanges coins | **Service: the bank**: accounts, notes, the village treasury ([economy](economy.md)) |

## 5. Interactions with the economy

Services make the economy richer rather than competing with it:

- **Every service is paid into the villager's balance** and taxed like a trade, so a village with a
  good librarian grows rich, and the treasury sees it.
- **Contracts are the economy's demand side.** A villager that wants wheat posts a contract only
  when it can pay for it; a poor village posts fewer.
- **Work costs materials villagers do not have** (lapis for enchanting, iron for repair): either
  the player brings them, or the villager buys them, which is a natural restocking sink.
- **Progression follows value**, as trades do, so a master is reached by doing business of any
  kind.

## 6. Where to be careful

- **Enchanting must not undercut the enchanting table into irrelevance.** Price by the vanilla book
  formula (`2 + rand(5 + 10·level) + 3·level` emeralds, doubled for `#double_trade_price`), only
  enchantments the villager knows, and vanilla's compatibility rules (`Enchantment.canEnchant`,
  exclusive sets), so it is a convenience and a choice, not a free upgrade.
- **Structure searches are expensive.** Vanilla's cartographer searches when the offer is rolled,
  and that is a known lag spike. An expedition can search when ordered, or spread the search over
  the days it is away, and must handle "nothing found" (refund).
- **Data files must not be able to mint money.** A work recipe's price and payout come from the
  same balances as trades, so a datapack shop cannot pay out more than the villager holds.
- **Keep vanilla reachable.** A profession without a definition still trades its vanilla offers,
  converted; services are opt-in per profession by data.

## 7. Decisions

*By the author, 18 Oct.*

- **The economy comes first** ([economy §8](economy.md#8-order)); services build on NPC balances.
- **Enchanting takes the item and money from the player; the librarian supplies the lapis.** The
  librarian buys lapis (from players, on its sell tab), spends it on each enchantment, and charges
  more the lower its lapis runs. So the first service is also the first dynamic price, and lapis
  keeps its value through the villager rather than through the table.

## 8. Suggested first steps

1. **Framework**: per-tab server actions, per-tab slots shown only for the selected tab, tab state
   saved by tab id, `min_level`, and the client renderer split. Each is small; together they are what
   every service needs.
2. **The pilot: the librarian's enchanting**, a Work tab. It exercises all of the framework (a slot,
   a payment, a refusal for incompatible items, progression) and has the clearest vanilla rules to
   copy.
3. **The cartographer's expedition**, an Order: the first service with saved state over days, and
   the first with real cost (the structure search) to manage.
4. Then the others as data, each a few lines of JSON once its tab type exists.
