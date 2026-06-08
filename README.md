# VSP - Very Simple PIN

A lightweight 4-digit PIN authentication plugin for [Paper](https://papermc.io) 26.1.2+.

Players authenticate using a clean chest GUI numpad.
---

## Features

- **PIN GUI** - chest-based numpad opens automatically on join
- **First-time setup** - new players set their PIN on first join
- **Fully locked until authenticated** - frozen, blinded, and protected from damage
- **Attempt limit** - players are kicked after 3 wrong attempts
- **Timeout** - players are kicked if they take too long to enter their PIN
- **SHA-256 hashed PINs** - PINs are never stored in plain text
- **Admin reset command** - reset any player's PIN from the console or in-game

---

## Installation

1. Download the latest `.jar` from [Releases](../../releases) or [Modrinth](https://modrinth.com/plugin/vsp).
2. Place the `.jar` in your server's `plugins/` folder.
3. Restart your server.

**Requirements:** Paper 26.1.2+ - Java 25+

---

## Usage

VSP requires all players to authenticate with a 4-digit PIN before interacting with the server. The PIN is set on first join and must be entered on every subsequent join.

### Players

On joining, a PIN numpad GUI opens automatically.

| Action | How |
|---|---|
| Enter a digit | Click a white concrete button |
| Delete last digit | Click the red **⌫ Delete** button |
| Submit | Click the green **✔ Confirm** button |

- **First join:** choose any 4-digit PIN and confirm it.
- **Subsequent joins:** enter your existing PIN.

### Administrators

| Command | Description |
|---|---|
| `/vspadmin reset <player>` | Clears a player's PIN, forcing them to set a new one |

Requires operator status (`op`).

---

## Configuration

PINs are stored in `plugins/VSP/pins.yml` as SHA-256 hashes.

The following constants can be changed by editing the source before compiling:

| Constant | Default | Description |
|---|---|---|
| `MAX_ATTEMPTS` | `3` | Wrong attempts before kick |
| `TIMEOUT_SECS` | `30` | Seconds to enter PIN before kick |

---

## Building from source

Requires Java 25 and Maven.

```bash
git clone https://github.com/bananasquare/VSP.git
cd VSP
mvn clean package
```

The compiled JAR will be in `target/VSP-1.0.0.jar`.

Alternatively, every push to `main` triggers a [GitHub Actions](.github/workflows/build.yml) build - download the artifact from the **Actions** tab.

---

## License

[Apache License 2.0](LICENSE) - bananasquare
