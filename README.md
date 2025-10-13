# Trade and Open Markets (TAOM)

**Trade and Open Markets** is a NeoForge mod that overhauls the entire Minecraft economy.  
The vanilla emerald economy is removed and replaced with a **multi-denomination coin system**, a wallet for storing currency, coin blocks for stacking and trading, and future systems for banking, trading, and server integrations.

---

## ✨ Current Features

- **Coins**: Multiple denominations (Copper, Bronze, Silver, Gold, Electrum, Platinum, Emerald).
- **Coin Blocks**: Place coins into the world as dynamic blocks that visually render coin piles.
    - Right-click to deposit coins.
    - Left-click to withdraw coins (highest denomination first).
    - Coins spill out when the block is broken.
- **Wallet**:
    - Stores coins in an internal balance (`CurrencyData`).
    - Auto-deposits coins as they are picked up.
    - Works in inventory or Curios slots.
    - Shift-right-click to open its menu.
- **Safes**: Secure block entity with planned advanced features for shared storage and trading.

---

## 🛠 Upcoming Work

- **Cashiers Checks**  
  Create tradeable paper notes backed by coin balances.

- **Coin Block Performance**  
  Fix large coin block drops to prevent lag or crashes when huge stacks break.

- **Trader Overhaul**  
  Replace vanilla villager trading with a new coin-based UI.

- **Safe Enhancements**  
  Expand safe block functionality with new permissions and features.

- **Bank Accounts**  
  Support persistent, player-linked balances beyond the wallet.

---

## 📦 Planned Long-Term Features

- **Villager Economy**: All villager trades will use coins instead of emeralds.
- **Server Integration**: Hooks to allow server owners to sell ranks, perks, or benefits for coins.
- **Custom Currency Items**: Add checks, bank notes, and other financial instruments.

---

## 🔧 Technical Notes

- Built on **NeoForge 1.21.1+**.
- Uses the new **Data Components** API to store wallet and coin data.
- Block entity rendering is custom (coin piles scale and thicken dynamically).
- Modular design allows additional denominations or new coin types to be added.

---

## 🚀 Contributing

This project is still in **early alpha**.  
Contributions, ideas, and testing feedback are welcome!

---

## 📋 License

All Rights Reserved
Future License option TBD
