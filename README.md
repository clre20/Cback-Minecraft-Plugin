# Cback

Cback 是一個基於 Paper 平台開發的玩家位置返回 (`/back`) 插件，支援多世界環境並整合 Floodgate API 提供 Java 版與基岩版雙端原生介面。

---

### 概述

Cback 提供玩家在發生傳送事件或死亡後返回先前位置的功能。針對 Geyser / Floodgate 跨平台架構進行底層優化，修正基岩版網路延遲與伺服器端防作弊拉回導致座標被錯誤覆蓋的問題。此外，插件提供 `/eback` 歷史紀錄選單，會依據客戶端來源自動派發 Java 箱子介面或基岩版原生表單。

### 功能架構

- **雙端介面適配**
  - 自動偵測客戶端連線協議。
  - Java 版：調用標準箱子容器介面。
  - 基岩版：透過 Floodgate 與 Cumulus API 調用客戶端原生按鈕表單。
- **座標防抖動與校驗機制**
  - **時間間隔限制 (`save-cooldown-ms`)**：設定最短儲存冷卻時間，過濾密集封包與短期連續位移。
  - **最小距離判定 (`min-distance`)**：同世界內小於指定距離的位移不予記錄，防止防作弊插件拉回修正時覆蓋真實傳送點。
- **歷史位置佇列**
  - 每個玩家最多保留 10 筆歷史座標。
  - 記錄項目包含觸發類型（傳送點 / 死亡點）、所屬世界、X/Y/Z 座標以及時間戳記。
- **傳送等待與中斷防護**
  - 支援傳送倒數計時。
  - 倒數期間可配置是否在玩家移動或受到傷害時中斷傳送。
- **效能與儲存**
  - 全面採用 Paper 異步傳送 API (`teleportAsync`)，降低跨世界或未載入區塊傳送對伺服器主執行緒的負載。
  - 玩家歷史紀錄採 UUID 獨立 YAML 檔案持久化儲存。
  - 設定檔具備缺漏鍵自動補齊功能，更新版本時保留現有客製化參數。

### 指令列表

| 指令 | 參數 | 預設權限 | 說明 |
| :--- | :--- | :--- | :--- |
| `/back` | 無 | 所有人 (`true`) | 返回上一次記錄的位置（傳送點或死亡點） |
| `/cback` | 無 | 所有人 (`true`) | 主要指令，功能同 `/back` |
| `/cback` | `reload` | 管理員 (`op`) | 重新載入設定檔與玩家歷史資料 |
| `/eback` | 無 | 所有人 (`true`) | 開啟歷史位置清單介面 |

### 權限節點

| 權限節點 | 預設指派 | 說明 |
| :--- | :--- | :--- |
| `cback.use` | 所有人 (`true`) | 允許執行 `/back`、`/cback` 及 `/eback` 指令 |
| `cback.admin` | 管理員 (`op`) | 允許執行 `/cback reload` 指令 |

### 設定檔說明 (`config.yml`)

```yaml
# 系統訊息前綴
prefix: "&8[&6Cback&8] "

# 傳送等待時間（秒）。設為 0 則立即傳送
teleport-delay-seconds: 5

# 傳送倒數期間移動是否取消傳送
cancel-teleport-on-move: true

# 傳送倒數期間受傷是否取消傳送
cancel-teleport-on-damage: true

# 儲存座標的最短冷卻時間（毫秒）
# 用於過濾 Geyser 封包延遲或防作弊回彈
save-cooldown-ms: 1000

# 同世界內觸發記錄的最小位移距離（格）
# 小於此距離的傳送事件將被忽略
min-distance: 3.0

# 是否記錄玩家死亡位置
record-death: true

# 允許觸發記錄的傳送類型
allowed-causes:
  - COMMAND
  - PLUGIN
  - NETHER_PORTAL
  - END_PORTAL
  - SPECTATE
```

---

### Overview

Cback is a Paper-based teleportation callback (`/back`) plugin designed to restore a player's previous location after teleportation or death. It addresses cross-platform coordinate desynchronization in Geyser / Floodgate environments, specifically mitigating packet latency issues and anti-cheat rubberbanding. The plugin also features an `/eback` menu that dynamically renders either a chest GUI or a native Bedrock form based on the client's platform.

### Technical Features

- **Platform-Aware UI Dispatching**
  - Determines client type at runtime.
  - Java Edition: Dispatches standard container inventory interface.
  - Bedrock Edition: Uses Floodgate and Cumulus APIs to render native button forms.
- **Position Filtering and Validation**
  - **Cooldown Threshold (`save-cooldown-ms`)**: Enforces a minimum interval between location writes to drop redundant or delayed packets.
  - **Distance Threshold (`min-distance`)**: Ignores positional changes within the same world that do not exceed the specified block distance, preventing anti-cheat setbacks from overriding valid coordinates.
- **Location History Queue**
  - Maintains a fixed-size queue of up to 10 entries per player.
  - Stores location metadata including event cause (teleport / death), world identifier, XYZ coordinates, and UNIX timestamps.
- **Warmup and Cancellation Handling**
  - Configurable teleport countdown.
  - Event listeners monitor player movement and damage during warmup to cancel pending teleports when configured.
- **Performance and Storage**
  - Uses Paper's `teleportAsync` API to prevent main-thread hangs during cross-world or unloaded-chunk teleports.
  - Persists player location data into individual YAML files indexed by player UUID.
  - Automatically merges new configuration entries on startup while preserving existing values.

### Commands

| Command | Arguments | Default | Description |
| :--- | :--- | :--- | :--- |
| `/back` | None | Everyone (`true`) | Teleports to the most recent location (teleport or death) |
| `/cback` | None | Everyone (`true`) | Primary command; aliases `/back` |
| `/cback` | `reload` | Operator (`op`) | Reloads `config.yml` and re-reads player data |
| `/eback` | None | Everyone (`true`) | Opens the location history interface |

### Permissions

| Permission | Default | Description |
| :--- | :--- | :--- |
| `cback.use` | Everyone (`true`) | Grants access to `/back`, `/cback`, and `/eback` |
| `cback.admin` | Operator (`op`) | Grants access to `/cback reload` |

### Configuration Reference (`config.yml`)

```yaml
# Chat and console log prefix
prefix: "&8[&6Cback&8] "

# Warmup delay in seconds. Set to 0 for instant execution
teleport-delay-seconds: 5

# Cancel teleport if the player moves during countdown
cancel-teleport-on-move: true

# Cancel teleport if the player takes damage during countdown
cancel-teleport-on-damage: true

# Minimum time (in milliseconds) required between saves
# Filters out rapid duplicate position updates or anti-cheat rubberbanding
save-cooldown-ms: 1000

# Minimum block distance within the same world required to record a new position
min-distance: 3.0

# Whether to record death coordinates
record-death: true

# Teleport causes that trigger position recording (PlayerTeleportEvent.TeleportCause)
allowed-causes:
  - COMMAND
  - PLUGIN
  - NETHER_PORTAL
  - END_PORTAL
  - SPECTATE
```
