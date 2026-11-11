# 🎣 FreshFishing

**Complete fishing overhaul for Paper servers** — sizes, rarities, economy integration, grappling hooks, and more. Perfect for survival, RPG, or any adventure server.

---

## ✨ Features

### Core Fishing
- 🐟 **Dynamic fish sizes** – configurable tiers (6–600 cm) with exponential distribution
- ⭐ **Rarity system** – custom names, colors, gradients, size multipliers (Default → Mystic)
- 🌊 **Biome restrictions** – limit fishing to specific biomes
- 💾 **Persistent data** – fish size & rarity survive death, buckets, restarts via PDC
- 🪣 **Bucket support** – fish retain attributes when bucketed/released
- 🎯 **Random entity catches** – catch mobs instead (squid, drowned, creepers, guardians, etc.)

### Economy & Trading
- 💰 **Fish market GUI** – sell catches for money (requires Vault/VaultUnlocked)
- 📊 **Dynamic pricing** – value scales with size & rarity; enchanted items worth more
- 💎 **Junk pricing** – shells, bottles, bones, saddles, fishing rods, etc. have set values

### Advanced
- 🪝 **Grappling hook** – use fishing rod to swing/pull on walls, ground, or entities
- 🦠 **Bait system** – optional: food in the off hand makes the catch bigger, the more hunger it restores the bigger the fish
- 📝 **Custom item lore** – show size & rarity using MiniMessage (colors, gradients)
- 🚀 **Physics-based flight** – caught mobs launch toward player with realistic velocity
- 🏜️ **Sand loot markers** – auto-populate suspicious sand/gravel with custom loot
- 🎨 **MiniMessage support** – full color/gradient/click event support

---

## 🎮 How It Works

1. **Spawning** – fish get random size tier & rarity based on weights
2. **Scaling** – visual size adjusts per tier & rarity multiplier
3. **Catching** – drops item with lore showing size & rarity
4. **Selling** – open GUI, sell for money (price = base + size bonus + enchantment bonus)
5. **Buckets** – stores metadata; releasing restores exact fish
6. **Mobs** – sometimes catch entities instead; they launch toward player
7. **Grappling** – right-click fishing rod to swing/pull with cooldown

---

## 📋 Commands & Permissions

### Fishing contest

- `/fish top [5|10|15]` shows the best catch per player. The default is 10.
- `/fish top reset` clears the contest for everyone and requires `freshfishing.admin`.

The ranking uses the fish shop's configured valuation at the time of the catch, including
rarity and the size after bait. With the default configuration, a 100 cm Default fish
scores 11.67 and a 50 cm Legendary fish scores 178.33. The earlier record wins ties.
FreshFishing starts tracking new catches when this version is installed.

By default records are stored in `plugins/FreshFishing/fishing-leaderboard.db`.
To use MySQL 8.0.19 or newer, set `leaderboard.storage.type: mysql`, fill in
`leaderboard.storage.mysql`, and restart. The configured database must already exist.
Commands and holograms read the same in-memory ranking, updated after successful writes.
Servers sharing MySQL pick up other servers' changes on the next refresh (every 15 seconds).
With BetterEffects installed, passing another player to take a top-five place plays a
typewriter action bar and a short flash. The flash uses the BetterEffects resource pack.

With MiniPlaceholders, use a line like this for each position in a hologram:

```text
<freshfishing_top:1:line>
```

Change the place number on each line. For custom formatting, the individual fields are
`player`, `cm`, `rarity`, `value`, and `place`. For a viewer's own record use
`<freshfishing_place>`, `<freshfishing_cm>`, and `<freshfishing_rarity>` in
a hologram that supports audience placeholders. Empty positions resolve to empty text.

Plugins can listen for
`ru.deelter.freshFishing.api.event.GrapplingHookUseEvent` and call
`event.setCancelled(true)` to stop the FreshFishing grapple for that use. The
event exposes the player and the bobber and leaves vanilla rod behavior in place.

| Command | Permission | Description |
|---------|-----------|-------------|
| `/fish menu [player]` | None | Open fish sale GUI |
| `/sandmarker create` | `freshfishing.admin` | Create sand loot marker |
| `/sandmarker list` | `freshfishing.admin` | List markers |
| `/sandmarker remove` | `freshfishing.admin` | Remove marker |
| — | `freshfishing.bypass` | Bypass hunger & biome restrictions |

---

## 🔧 Configuration

Everything in `config.yml` – fully commented. Key sections:

### Hunger
```yaml
hunger:
  min-level-for-fishing: 3
  fish: 1      # Cost per fish
  item: 0      # Cost per junk
```

### Rarities (adjust blocks for weight, multiplier for value/size)
```yaml
rarities:
  default:
    name: "<color:#D5D5D5>Default</color>"
    blocks: 100
    multiplier: 1.0
  legendary:
    name: "<color:#FFD700>Legendary</color>"
    blocks: 2
    multiplier: 1.4
```

### Size Tiers (lambda = distribution shape)
```yaml
sizes:
  tier1:
    min: 6.0
    max: 16.0
    blocks: 120
    lambda: 4    # Higher = clustered near min
```

### Sale Prices
```yaml
fish-sale-ranges:
  default:
    min-price: 8
    max-price: 450
  legendary:
    min-price: 150
    max-price: 3000
enchantment-prices:
  MENDING: 40.0
  LURE: 15.0
```

### Grappling Hook
```yaml
grappling-hook:
  enabled: true
  wall-grapple: true
  cooldown-ticks: 40
  pull-strength: 1.0
```

### Bait System
```yaml
bait:
  size-bonus-per-nutrition: 2.0   # +% fish size per hunger point of the off-hand food
  consume-on-catch: true
```

### Item Lore
```yaml
item-lore:
  - "<color:#CBFF61>Size:</color> <size>cm"
  - "<color:#CBFF61>Rarity:</color> <rarity>"
```

### Entities to Catch
```yaml
entities:
  COD: 65
  SALMON: 60
  SQUID: 8
  CREEPER: 1
  GUARDIAN: 3
```

---

## 💡 Tips

- **Test colors** – use [MiniMessage Web UI](https://webui.advntr.dev/)
- **Find entity types** – [Bukkit EntityType docs](https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/EntityType.html)
- **Disable feature** – set to empty list or 0 (e.g., `hunger: {fish: 0, item: 0}`)
- **Economy required** – install [Vault](https://www.spigotmc.org/resources/vault.41918/) for selling

---

## 📦 Installation

1. Download `FreshFishing.jar`
2. Place in `plugins/` folder
3. Restart server
4. Edit `config.yml` to taste

---

## 🛠️ Requirements

- **Paper 1.21+**
- **Vault or VaultUnlocked** (for economy)
- Java 17+

---

## 🤝 Support

- **GitHub**: [FreshFishing](https://github.com/DeelTer/FreshFishing)
- **Issues**: [GitHub Issues](https://github.com/DeelTer/FreshFishing/issues)
- **Author**: [DeelTer](https://t.me/deelter)

---

## 📄 License

MIT – see [LICENSE](LICENSE) file.
