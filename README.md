**Compact F3 Plus** is a client-side NeoForge mod for Minecraft 1.21.1 that replaces the dense, cluttered vanilla F3 debug screen with a clean and minimal HUD overlay. It displays only the essential information you need, anchored to the top left of your screen on a sleek transparent background.

*(This is an actively maintained fork of the original "Compact F3" by username65735).*

## ✨ Features

- **Essential Metrics:** Neatly tracks FPS, Server TPS/MSPT, exact XYZ coordinates, movement speed (km/h), facing direction, in-game time, light levels, and biomes.
- **Dynamic Crosshair Compatibility:** Our unique render event implementation temporarily pauses vanilla UI occlusion, guaranteeing that this mod stays 100% compatible with custom crosshair mods such as **Dynamic Crosshair**.
- **Highly Customizable:** Ships with a native in-game configuration menu (available in the Mod List). Toggle specific lines on or off, enable colored health/TPS indicators, or hide the center 3D XYZ gizmo.
- **F3 Replacement:** Automatically syncs with vanilla inputs. Press F3 to toggle the compact overlay seamlessly out of the box, or bind it to a custom hotkey like `F8`.

## 📦 Installation
Requirements:
* **Minecraft 1.21.1**
* **NeoForge Loader**

Simply drop the `.jar` file into your `.minecraft/mods` folder!

## 🛠️ Building from Source
Clone the repository and run the standard Gradle wrapper via Command Line/Git Bash:
```bash
./gradlew build
```
The compiled mod will be located in `/build/libs/`.