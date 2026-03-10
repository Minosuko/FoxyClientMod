# 🦊 FoxyClientMods

FoxyClientMods is an advanced, custom-built Minecraft client modification designed for extensive game manipulation, combat utility, visual enhancements, and exploit testing across modern versions of Minecraft. 

---

## ✨ Features and Modules

FoxyClient includes categorized sets of tools and mechanics. Press `Right Shift` (default) to open the ClickGUI and configure these dynamically during gameplay.

---

## 🚀 Installation & Build Guide

### Prerequisites
To build and use FoxyClient, make sure you have the following installed:
- [Java 21 JDK](https://adoptium.net/) (or the specific version required by your target Minecraft build)
- [Fabric Loader](https://fabricmc.net/) installed on your client

### Compiling from Source
FoxyClient uses the standard Gradle wrapper for building the project environment.

1. Clone or download the source code wrapper.
2. Open a terminal in the root directory and run the compile task:

```bash
# Windows
.\gradlew.bat build

# Linux / MacOS
./gradlew build
```

3. Locate the compiled `.jar` file output inside the `build/libs/` directory.

### Running the Client
1. Drop the compiled `foxyclient-[version].jar` into your Minecraft `.minecraft/mods` folder.
2. Ensure you have the corresponding `fabric-api` version installed alongside it.
3. Launch the game using your configured Fabric launcher profile.

---

## 📜 Development Disclaimer

**FoxyClientMods is an educational project.** 
The codebase contains powerful testing tools and mechanics that manipulate network protocols, client synchronization, and generic Minecraft gameplay. 

Please use responsibly and ensure you have explicit sandbox permission from server administrators before deploying and testing exploit or combat modules in active multiplayer environments. No liability is assumed for account sanctions or server penalties resulting from misuse.
