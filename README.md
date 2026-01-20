# 🌊 Waves Framework

[![Code Quality](https://www.codefactor.io/repository/github/mrlarkyy/waves/badge)](https://www.codefactor.io/repository/github/mrlarkyy/waves)
[![Reposilite](https://repo.nekroplex.com/api/badge/latest/releases/gg/aquatic/Waves?color=40c14a&name=Reposilite)](https://repo.nekroplex.com/#/releases/gg/aquatic/Waves)
![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-purple.svg?logo=kotlin)
[![Discord](https://img.shields.io/discord/884159187565826179?color=5865F2&label=Discord&logo=discord&logoColor=white)](https://discord.com/invite/ffKAAQwNdC)

**Waves** is a cutting-edge, modular development ecosystem for high-scale Minecraft servers. Built from the ground up to
leverage **Kotlin Coroutines** and **Packet-Level Abstractions**, it allows developers to build feature-rich
experiences (like fake entities, custom GUIs, and scripted logic) without the performance tax of traditional Bukkit API
implementations.

---

## 🏛 Core Philosophy: "Client-Side First"

Traditional Minecraft development relies on server-side entities that tick every 50ms, consuming valuable CPU cycles.
Waves shifts the paradigm:

- **Zero-Tick Visuals**: Using the `Pakket` module, visuals like Holograms and Fake Entities exist only in the player's
  memory.
- **Asynchronous Logic**: Nearly every system in Waves (Actions, Database, Input) is designed to run on `Dispatchers.IO`
  or custom coroutine scopes, keeping the main server thread focused strictly on physics and vital logic.
- **Snapshot Caching**: High-read data is stored in `SnapshotMap`, providing near-instant access for multi-threaded read
  operations.

---

## 🧱 The Module Ecosystem

Waves is powered by a family of libraries. You can use the full suite or pick individual modules.

| Module                                                         | Purpose                                                                                                    | Repository                                             |
|:---------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------|:-------------------------------------------------------|
| **[AquaticCommon](https://github.com/MrLarkyy/AquaticCommon)** | The foundation. Includes `ArgumentContext` for flexible data passing and robust `Ticker` implementations.  | [🔗 Source](https://github.com/MrLarkyy/AquaticCommon) |
| **[Pakket](https://github.com/MrLarkyy/Pakket)**               | A high-level NMS/Packet wrapper. Handles entity spawning, metadata updates, and passenger management.      | [🔗 Source](https://github.com/MrLarkyy/Pakket)        |
| **[Execute](https://github.com/MrLarkyy/Execute)**             | A serializable logic engine. Allows `Actions` (rewards) and `Requirements` (checks) to be defined in YAML. | [🔗 Source](https://github.com/MrLarkyy/Execute)       |
| **[KMenu](https://github.com/MrLarkyy/KMenu)**                 | A DSL for Inventories. Supports reactive updates, pagination, and asynchronous click handling.             | [🔗 Source](https://github.com/MrLarkyy/KMenu)         |
| **[Kommand](https://github.com/MrLarkyy/Kommand)**             | Type-safe command routing. No more massive `onCommand` switch statements.                                  | [🔗 Source](https://github.com/MrLarkyy/Kommand)       |
| **[SnapshotMap](https://github.com/MrLarkyy/SnapshotMap)**     | Thread-safe, lock-free read map designed for high-concurrency environments.                                | [🔗 Source](https://github.com/MrLarkyy/SnapshotMap)   |
| **[KLocale](https://github.com/MrLarkyy/KLocale)**             | Internationalization engine. Supports per-player languages via Adventure/MiniMessage.                      | [🔗 Source](https://github.com/MrLarkyy/KLocale)       |

---

## 🛠 Feature Deep-Dive

### 1. Fake Entity & Block System (`gg.aquatic.waves.clientside`)

Easily manage complex visuals that don't affect server TPS.

- **FakeEntity**: Supports all EntityTypes, equipment, and metadata.
- **Multi-Blocks**: Display complex structures using fake blocks that can be changed dynamically for specific "
  audiences."
- **ModelEngine Support**: Seamlessly integrate `FakeMEG` to handle custom models as packet-entities.

### 2. Scriptable Logic (`gg.aquatic.execute`)

The `Execute` module allows you to turn configuration files into executable code.

- **Arguments**: Define required/optional parameters with default values.
- **Contextual Execution**: Actions can be bound to players or console.
- **Logical Actions**: Use `ConditionalActionsAction` to create "if-then-else" logic directly in your plugin's config.

### 3. Modern GUI Design (`gg.aquatic.kmenu`)

Stop fighting with `InventoryClickEvent`.

```kotlin
val menu = kMenu(rows = 3) {
    title = "Select a Reward".miniMessage()

    button(13, itemStack(Material.DIAMOND)) {
        onClick { player ->
            player.sendMessage("You clicked the diamond!")
        }
    }
}
```

---

## ⚡ Performance Benchmarks

The **SnapshotMap** module is specifically benchmarked for Minecraft environments.

- **Read Speed**: Significantly faster than `ConcurrentHashMap` for high-frequency lookups (e.g., getting player data on
  every move packet).
- **Scalability**: Designed to maintain performance as player counts increase.

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Gradle (with Kotlin DSL)
- A Paper/Spigot server 1.21.1+

### Installation

Clone with submodules to ensure all internal libraries are present:

```shell script
git clone --recursive https://github.com/MrLarkyy/Waves.git
```

### Building

```shell script
# Build the main shadowed jar
./gradlew shadowJar
```

## 🤝 Contributing

Waves is a massive ecosystem. If you'd like to contribute to a specific module, please check the individual repository
links in the table above.

---

## 💬 Community & Support

Got questions, need help, or want to showcase what you've built with **Waves**? Join our community!

[![Discord Banner](https://img.shields.io/badge/Discord-Join%20our%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/invite/ffKAAQwNdC)

* **Discord**: [Join the Aquatic Development Discord](https://discord.com/invite/ffKAAQwNdC)
* **Issues**: Open a ticket on GitHub for bugs or feature requests.