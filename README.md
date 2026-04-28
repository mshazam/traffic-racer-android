# Traffic Racer

A thrilling, fast-paced traffic racing game for Android built with Kotlin and Canvas-based rendering. Dodge traffic, collect coins, grab power-ups, and chase high scores!

## Features

### Gameplay
- **4-Lane Highway** — Smooth lane-switching with intuitive swipe or tap controls
- **Dynamic Traffic** — Multiple vehicle types (sedans, SUVs, trucks, sports cars, buses) with varying speeds and sizes
- **Increasing Difficulty** — Speed ramps up over time, traffic gets denser
- **Near-Miss Bonus System** — Get bonus points for close calls, with combo multipliers up to 10x
- **Day/Night Cycle** — Transitions to night mode as your score climbs

### Power-Ups
- **Shield** — Absorb one collision without losing a life
- **Magnet** — Attract nearby coins automatically
- **2x Score** — Double all points earned
- **Extra Life** — Gain an additional life (up to 5 max)
- **Nitro Boost** — Burst of speed with bonus points and flame effects

### Controls
- **Swipe Left/Right** — Change lanes
- **Swipe Up** — Activate nitro boost
- **Tap Left/Right Side** — Quick lane change
- **Pause Button** — Top-right of HUD

### Visual Effects
- Particle effects on collisions, coin collection, and power-up activation
- Boost flame animation behind the player car
- Shield aura effect
- Screen shake on crashes
- Animated scrolling road markings and scenery
- Coin spin animation with collection pop effect
- Power-up glow and pulse effects
- Speed lines during nitro boost
- Smooth car tilt when switching lanes

### Audio & Haptics
- Sound effects for lane switches, coin pickups, crashes, boosts, and power-ups
- Haptic feedback on collisions and coin collection
- Varying vibration intensity for different events

### Score & Progress
- High score persistence with SharedPreferences
- Detailed game-over stats (distance, coins, total score)
- "New High Score" celebration effect

## Tech Stack

- **Language:** Kotlin
- **Graphics:** Android Canvas with SurfaceView
- **Audio:** ToneGenerator + SoundPool
- **Haptics:** Android Vibrator API
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)

## Building

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Steps
1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Run on a device or emulator

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
app/src/main/java/com/trafficracer/game/
├── MainActivity.kt       — App entry point, immersive mode setup
├── GameView.kt           — SurfaceView, game thread, touch input handling
├── GameWorld.kt          — Game logic, physics, collision, state management
├── GameRenderer.kt       — All Canvas drawing (road, cars, HUD, screens, effects)
├── Entities.kt           — Data classes for all game objects
├── Constants.kt          — Game constants, enums (GameState, CarType, PowerUpType)
├── SoundManager.kt       — Audio effects and haptic feedback
└── HighScoreManager.kt   — Score persistence via SharedPreferences
```

## License

MIT
