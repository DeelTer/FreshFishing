# 🎣 FreshFishing

**A complete fishing overhaul for Paper servers – unique fish sizes, rarities, hunger cost, biome restrictions, and random entity catches.**

---

## ✨ Features

- **Fish sizes** – configurable size tiers (cm) with exponential distribution
- **Rarities** – custom names, colors, and size multipliers
- **Biome restrictions** – fish only in allowed biomes (e.g. oceans, rivers)
- **Hunger system** – consumes hunger when fishing; configurable costs
- **Random entities** – catch mobs instead of fish (squid, drowned, guardians, etc.)
- **Custom item lore** – displays size and rarity using MiniMessage
- **Persistent data** – fish size and rarity stored in PDC (survives death, buckets)
- **Bucket support** – fish retain their attributes when bucketed/released
- **Physics** – caught entities fly toward the player with configurable velocity

---

## 📋 Commands & Permissions

This plugin has **no commands** – it works fully automatically.

Permissions:
- `freshfishing.bypass` – bypass biome & hunger restrictions (default: op)

---

## 🔧 Configuration

All settings are in `config.yml`. Detailed comments inside the file explain every option.

### Quick examples:

**Biome restriction** – `fish-only-in-biomes.list`  
**Hunger cost** – `hunger.fish` / `hunger.item`  
**Rarities** – add/remove, change colors, adjust weights (`blocks`)  
**Sizes** – add new tiers, adjust lambda for distribution  
**Entities** – add any `EntityType` (e.g. `SHULKER: 1`)

---

## 🎮 How it works

1. When a fish spawns (naturally or via fishing), a random size tier and rarity are chosen based on configured weights.
2. The fish's scale (visual size) is adjusted accordingly.
3. When caught/killed, the fish drops an item with lore showing its size and rarity.
4. Bucketing stores the attributes; releasing restores them.
5. Fishing may also spawn other entities (e.g. a guardian or skeleton) with custom velocity.

---

## 📦 Installation

1. Download `FreshFishing.jar`
2. Place it in your server's `plugins/` folder
3. Restart the server or use `/plugman load FreshFishing`
4. Edit `config.yml` to your liking

---

## 🛠️ Requirements

- **Paper 1.21.3+** (or any Paper fork)
- Java 21+

---

## 📝 Notes

- Placeholders in `item-lore`: `<size>` (numeric size in cm), `<rarity>` (colored name from config)
- All text formatting uses **MiniMessage** – test at [https://webui.advntr.dev/](https://webui.advntr.dev/)
- Leave a setting empty to disable certain features (e.g. `list: []` to allow all biomes)

---

## 📄 License

MIT – see [LICENSE](LICENSE) file.

---

**Issues:** [GitHub Issues](https://github.com/DeelTer/FreshFishing/issues)
