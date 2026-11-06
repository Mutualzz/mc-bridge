<div align="center">

# Mutualzz is developed at [git.mutualzz.com](https://git.mutualzz.com/mutualzz/mc-bridge)

**GitHub is a read-only mirror. Issues and pull requests opened on GitHub are not monitored.**

Sign in with your GitHub account. No new password needed.

</div>

# Mutualzz Bridge (server plugin)

Connects your **Paper** Minecraft server to **Mutualzz** — chat, account linking, and voice presence with Discord and the Mutualzz app.

**One plugin jar** supports **Paper/Spigot 1.18.2 through current** (including Paper 26.x). In-game mic/speakers still need a **Mutualzz Voice** client jar matching the player’s **Minecraft version and loader** (Fabric, NeoForge, or Forge).

Players can:

- Link their Mutualzz account (`/mzlink`)
- Share chat with Discord / the Mutualzz app
- Join Mutualzz voice from in-game (`/mzvoice`)

In-game **mic and speakers** need the [Mutualzz Voice](https://git.mutualzz.com/mutualzz/mc-voice) mod on each player’s client — the jar must match their **MC version + loader** (Fabric / NeoForge / Forge). Presence-only voice works without the mod.

The plugin talks to **Mutualzz’s hosted bridge hub**. You do not run your own hub.

---

## License & redistribution

Source is available for transparency and **community contributions**. That is **not** permission to redistribute builds.

**You may:**

- Install official builds on your Paper server and connect them to Mutualzz with your bridge token
- Fork the repo and open pull requests — contributors get credit (see [`CONTRIBUTING.md`](https://git.mutualzz.com/mutualzz/mc-bridge/src/branch/master/CONTRIBUTING.md))

**You may not** (without written permission from Mutualzz):

- Re-upload or share general-purpose jars / unofficial builds
- Publish it on Modrinth, CurseForge, GitHub Releases, or any other site for others to install
- Run or sell a third-party “Mutualzz-compatible” hub

See [`LICENSE`](https://git.mutualzz.com/mutualzz/mc-bridge/src/branch/master/LICENSE). Contact [mutualzz.com](https://mutualzz.com) for partnership rights.

---

## What you need

|          |                                                                                                                                     |
| -------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Server   | **Paper (or compatible) 1.18.2+** — tested focus: 1.18.2, 1.21.x, 26.x                                                              |
| Java     | **17+** (Paper’s requirement for that MC line; newer Paper needs 21/25)                                                             |
| Mutualzz | A **space** with Manage Space permission, and a bridge created under **Space Settings → Minecraft Bridge**, with a **bridge token** |
| Optional | Discord linked in Mutualzz; voice rooms bound to this server                                                                        |

---

## Install (server owners)

### 1. Create a bridge in Mutualzz

1. Open Mutualzz → open the space → **Space Settings → Minecraft Bridge**.
2. Create a bridge and copy the **token** (starts with `mz_bridge_…`).
3. Choose a **server ID** (e.g. `survival`) — use the same ID when binding Discord channels and voice rooms in Mutualzz.

### 2. Add the plugin

1. Download the official bridge jar from [Modrinth](https://modrinth.com) or the [official release](https://git.mutualzz.com/mutualzz/mutualzz/releases) titled **Mutualzz Minecraft v…** (tag `minecraft-v…`) — not unofficial mirrors. The same release includes the Voice mod jar(s) for supported client versions.
2. Drop `mutualzz-bridge-*.jar` into your server’s `plugins` folder.
3. Start (or restart) the server once so it creates the config folder.

### 3. Configure

Edit `plugins/MutualzzBridge/config.yml`:

```yaml
# Official Mutualzz hub — leave this as-is
hubUrl: wss://bridge.mutualzz.com

# Paste the token from Mutualzz settings
token: mz_bridge_your_token_here

# Same ID you use when binding Discord / voice in Mutualzz
serverId: survival

# Sync player inventories to Mutualzz (see "Inventory sync" below)
inventorySync: true
```

### 4. Restart and finish setup in Mutualzz

1. Restart the server after saving config.
2. In Mutualzz, open **Space Settings → Minecraft Bridge** for this space:
   - Link Discord (if you use it)
   - Bind chat / voice rooms to this `serverId`
3. You’re done on the server side.

### 5. Players

1. Join the Minecraft server.
2. Run `/mzlink` and enter the code from Mutualzz (or `/mzlink <code>`).
3. Voice: `/mzvoice join` or `/mzvoice join <room>` — leave with `/mzvoice leave`.
4. For **hearing people in-game**, install the [Mutualzz Voice](https://git.mutualzz.com/mutualzz/mc-voice) jar for **your Minecraft version and loader** (Fabric needs Fabric API + Amecs on 26.1.2; NeoForge/Forge do not).

In-game voice defaults use **Alt** (not Ctrl) — e.g. **Alt+O** settings, **Alt+M** mute, hold **V** for push-to-talk. Full list: [Mutualzz Voice controls](https://git.mutualzz.com/mutualzz/mc-voice#controls).

---

## Commands

| Command                | Who     | What it does                               |
| ---------------------- | ------- | ------------------------------------------ |
| `/mzlink`              | Players | Start linking to Mutualzz                  |
| `/mzlink <code>`       | Players | Finish linking with a code from Mutualzz   |
| `/mzvoice join [room]` | Players | Join Mutualzz voice (`default` if no room) |
| `/mzvoice leave`       | Players | Leave Mutualzz voice                       |

Permissions (default: everyone can use them):

- `mutualzz.bridge.link`
- `mutualzz.bridge.voice`

---

## Inventory sync

With `inventorySync: true` (the default), the plugin sends each player's inventory and ender chest to Mutualzz:

- when they leave the server,
- every few minutes while they're online,
- live while someone is viewing it in Mutualzz.

This applies to **every player on the server**, including players without a Mutualzz account. Space admins of the connected space can view any player's saved inventory; players can view their own after `/mzlink`. Snapshots are deleted when the bridge is deleted.

To turn it off, set `inventorySync: false` in `config.yml` and restart the server.

## Remote moderation

With `remoteModeration: true`, admins of the connected Mutualzz space can kick and ban players on this server from the app. Off by default — space admins are not automatically Minecraft operators.

Bans run `banCommand` / `unbanCommand` as the console, so ban plugins (EssentialsX, LiteBans, …) keep working. Adjust the commands if your plugin uses different ones. Every remote command is written to the server log.`

Servers that were offline when a ban or unban happened catch up the next time they connect. If you turn `remoteModeration` on later, bans made in Mutualzz while it was off are applied on the next connect.

## Remote inventory editing

With `remoteInventoryEdit: true`, space admins can give, remove, move and change the amount of items in player inventories from the app — also for offline players (applied when they next join). Off by default.

Every edit is checked against the slot's current state when it's applied: items the server already knew are compared byte for byte, and items created by an earlier edit in the same queue are compared by item type. If the inventory changed in the meantime, that edit and every later one are rejected instead of applied, so items can't be duplicated. Queued edits for offline players are applied together, in order, in a single tick when they join. Every edit is written to the server log.

- [ ] Official plugin jar in `plugins/`
- [ ] `config.yml` has `hubUrl: wss://bridge.mutualzz.com`, your `token`, and `serverId`
- [ ] Server restarted after editing config
- [ ] Bridge shows connected in Mutualzz
- [ ] Discord / voice rooms bound to this `serverId` (if you use them)
- [ ] Players linked with `/mzlink`
- [ ] (Optional) Players installed Mutualzz Voice **for their MC version + loader** for in-game audio

---

## Troubleshooting

- **Bridge won’t connect** — Check the token is correct and the server can reach `wss://bridge.mutualzz.com` (outbound WebSocket).
- **Plugin disables on enable** — Server is older than **1.18.2**.
- **Chat doesn’t appear in Discord** — Bind the Discord channel to this bridge’s `serverId` in Mutualzz.
- `/mzvoice` **says not linked** — Player needs `/mzlink` first.
- **Joined voice but silent in Minecraft** — Expected without Mutualzz Voice, or with a jar that doesn’t match the client’s MC version + loader.
- **Wrong** `serverId` — Must match what you set when binding rooms in Mutualzz.
