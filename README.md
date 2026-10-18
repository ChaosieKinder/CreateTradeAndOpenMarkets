# Errant Trade and Markets

**Errant Trade and Markets** is a NeoForge mod that overhauls the entire Minecraft economy.  
The vanilla emerald economy is removed and replaced with a **multi-denomination coin system**, a wallet for storing currency, coin blocks for stacking and trading, and future systems for banking, trading, and server integrations.

---

## ✨ Current Features

- **Coins**: Multiple denominations
- **Coin Blocks**: Place coins into the world as dynamic blocks that visually render coin piles.
    - Right-click to deposit coins. Shift for a whole stack, double use with shift to deposit all. 
    - Left-click to withdraw coins (highest denomination first). shift for more withdraws per click.
    - Coins spill out when the block is broken, too many coins will result in a single "coin pile" being dropped
      instead to reduce lag. The coin pile can be placed as normal, or added to an existing coin block. While 
      the coin piles can be used to carry arbitrary amounts of currency, they do not stack, paper notes are 
      recommended for that use.
    - Breaks if the block underneath it is broken
- **Coin Stack (Coin Block Item)**
    - Only obtainable from breaking a large coin block. Main purpose is to prevent lag or crashing if a large
       Coin Block breaks (due to potentially thousands of stacks of items being made)
- **Wallet**:
    - Stores coins and displays total value. Allows withdrawing coins as needed. 
    - Auto-deposits coins as they are picked up.
    - Works in inventory or Curios slots.
    - Shift-right-click to open its menu.
    - Also acts as a 9-slot storage for security cards, cashiers checks, etc.
- **Safe**: Secure block entity with planned advanced features for shared storage and trading.
- **ID Card**: Stores a players identity for use with various parts of the mod. 

---
## 🛠 Upcoming Work

- **Trader Overhaul**
    - Replace vanilla villager trading with a new coin-based UI.

- **Banker Villager**
    - New villager, uses the safe as a job block.
    - Sells a few things, access's bank accounts, and changes money

- **Bank Accounts**
    - Support persistent, player-linked balances beyond the wallet.

- **Paper Notes**
  - Paper notes can be deposited to banks and wallets directly
  - Create tradeable paper notes backed by coin balances.
  - Can only be deposited/created at banks
  
- **Coin Stack Item**  
  - Can be used in crafting to withdraw stacks at a time until empty

- **Safe Enhancements**  
  - Expand safe block functionality with new permissions and features.
    - Deposit slots
    - Access cards
    - Redstone output when opened by non-authed players
    - Hidden safes

- **Enchantments**
  - several enchantments for wallet, safe block, and others
    - Wallet
      - Soulbound
    - Safe
      - Protection
      - Blast Protection
---

## 📦 Planned Long-Term Features

- **Server Integration**: Hooks to allow server owners to sell ranks, perks, or benefits for coins.
  - Integration with land claiming mods for purchase/tax on claims
- **Custom Currency Items**: Add checks, bank notes, and other financial instruments.
- **Inflation detection and prevention**:
- **Counterfeiting? (can be disabled)**:

---

## 🔧 Compatibility

- Built on **NeoForge 1.21.1+**.
Hard dependencies have been avoided but several mods are required for specific functionality.
- Not-quite-required-but-reccomended
  - JEI (obviously)
  - Curios (for wallet slot)
  - Create
    - (planned) Ponder support
    - (planned) Some recipes change dramatically with create installed. While not required, tweaking crafting is highly reccomended if installed to a modpack without create.
- Built in compatibility with several mods. None are hard requirements.
  - (planned) Tombstone Soulbound
  - (planned) Securitycraft
- Compatibility with villagers added by several mods (more coming)
  - (planned) Pneumatic Craft
  - (planned) Ars


---

## 🚀 Contributing

This project is still in **early alpha**.  
Contributions, ideas, and testing feedback are welcome!

---

## 📋 License

All Rights Reserved
Future License option TBD
