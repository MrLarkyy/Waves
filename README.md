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

Waves is powered by a comprehensive family of specialized libraries. Each module handles a specific pillar of modern
plugin development, allowing for a clean, decoupled architecture.

| Module                                                         | Purpose                                                                                                                           | Repository                                             |
|:---------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------|
| **[AquaticCommon](https://github.com/MrLarkyy/AquaticCommon)** | The foundation. Provides `ArgumentContext` for type-safe data parsing, Coroutine scopes, and extensive Bukkit extensions.         | [🔗 Source](https://github.com/MrLarkyy/AquaticCommon) |
| **[Pakket](https://github.com/MrLarkyy/Pakket)**               | High-level NMS/Packet abstraction. Manages client-side entity spawning, metadata updates, and passenger packets.                  | [🔗 Source](https://github.com/MrLarkyy/Pakket)        |
| **[Execute](https://github.com/MrLarkyy/Execute)**             | A serializable logic engine. Allows complex `Actions` and `Requirements` to be defined in configurations and executed at runtime. | [🔗 Source](https://github.com/MrLarkyy/Execute)       |
| **[KMenu](https://github.com/MrLarkyy/KMenu)**                 | A reactive DSL for Inventory GUIs. Features asynchronous click handling, pagination, and dynamic button updating.                 | [🔗 Source](https://github.com/MrLarkyy/KMenu)         |
| **[Kommand](https://github.com/MrLarkyy/Kommand)**             | Modern, type-safe command routing framework that eliminates boilerplate command registration.                                     | [🔗 Source](https://github.com/MrLarkyy/Kommand)       |
| **[KLocale](https://github.com/MrLarkyy/KLocale)**             | Internationalization engine. Handles per-player localization with full Adventure/MiniMessage support.                             | [🔗 Source](https://github.com/MrLarkyy/KLocale)       |
| **[SnapshotMap](https://github.com/MrLarkyy/SnapshotMap)**     | Thread-safe, lock-free maps optimized for extreme read performance in high-concurrency environments.                              | [🔗 Source](https://github.com/MrLarkyy/SnapshotMap)   |
| **[KRegistry](https://github.com/MrLarkyy/KRegistry)**         | Dynamic object registry for managing lifecycles and lookups of custom plugin components.                                          | [🔗 Source](https://github.com/MrLarkyy/KRegistry)     |
| **[KEvent](https://github.com/MrLarkyy/KEvent)**               | Lightweight, Coroutine-friendly event wrappers to replace standard, bulky event listeners.                                        | [🔗 Source](https://github.com/MrLarkyy/KEvent)        |
| **[Kurrency](https://github.com/MrLarkyy/Kurrency)**           | A unified economy abstraction layer supporting multiple providers (Vault, PlayerPoints, etc.) and custom currencies.              | [🔗 Source](https://github.com/MrLarkyy/Kurrency)      |
| **[Blokk](https://github.com/MrLarkyy/Blokk)**                 | Advanced block manipulation library for handling custom block data and complex block-based mechanics.                             | [🔗 Source](https://github.com/MrLarkyy/Blokk)         |
| **[Replace](https://github.com/MrLarkyy/Replace)**             | High-performance string replacement engine designed for rapid placeholder processing.                                             | [🔗 Source](https://github.com/MrLarkyy/Replace)       |
| **[Stacked](https://github.com/MrLarkyy/Stacked)**             | Modern ItemStack utility library for simplified NBT access, PersistentData management, and item building.                         | [🔗 Source](https://github.com/MrLarkyy/Stacked)       |
| **[Statistik](https://github.com/MrLarkyy/Statistik)**         | Optimized data tracking system for player metrics, statistics gathering, and persistent data storage.                             | [🔗 Source](https://github.com/MrLarkyy/Statistik)     |
| **[TreePAPI](https://github.com/MrLarkyy/TreePAPI)**           | An efficient integration and caching layer for PlaceholderAPI, reducing the overhead of placeholder requests.                     | [🔗 Source](https://github.com/MrLarkyy/TreePAPI)      |

---

## 🛠 Feature Deep-Dive

### 👻 Packet-Based "Fake" Objects

Located in `gg.aquatic.waves.clientside`, this system allows for:

- **Client-Side Entities**: Spawn NPCs, items, or effects that only specific players can see.
- **ModelEngine Integration**: Built-in support for `FakeMEG` to handle custom models via packets.
- **Interaction Handling**: Packet-level click detection that maps back to standard Bukkit-like events.

### 📜 Scriptable Actions (`Execute`)

Turn your YAML configs into logic. `Execute` supports:

- **Arguments**: Type-safe parameter parsing (Int, String, Collection, etc.) for actions.
- **Requirements**: Gate actions behind checks (permissions, currency, etc.).
- **Smart Actions**: Nested logical conditions directly in configuration.

### 💰 Economy & Data (`Kurrency` & `Statistik`)

- **Kurrency**: A unified API for handling multiple types of currency (Vault, Points, etc.).
- **Statistik**: Optimized tracking of player data and metrics.

### 🗺 Optimized Collections (`SnapshotMap`)

For data that is read thousands of times per second (like move events or packet listeners), Waves uses `SnapshotMap` to
provide lock-free, thread-safe access that outperforms standard `ConcurrentHashMap`.

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