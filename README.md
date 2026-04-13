# ⚡ Rock · Paper · Scissors — Java Swing Game

A modern, visually stunning Rock Paper Scissors game built with **Java Swing**.  
Features animated glow buttons, live score tracking, and a sleek dark-mode UI.

---

## 🎮 Features

| Feature | Details |
|---|---|
| 🎨 **Dark UI** | Premium dark theme with purple / cyan / pink accents |
| ✨ **Glow Animations** | Pulsing button glow effects and result flash animations |
| 🏆 **Score Tracking** | Win / Lose / Tie counters that persist across rounds |
| ⏳ **CPU "Thinking" Delay** | 0.7s suspense animation before CPU reveals its choice |
| 📜 **Round History** | Last 10 rounds shown as color-coded W/L/T badges |
| 🔄 **Reset Button** | One-click full game reset |
| 📐 **Resizable Window** | Fully responsive layout |

---

## 🚀 How to Run

### Option 1 — Double-click (Easiest)
1. Double-click **`compile_and_run.bat`**
2. The script will automatically find your JDK, compile, and launch the game.

### Option 2 — Manual (Command Prompt / PowerShell)
```bash
# Step 1: Compile
javac RockPaperScissors.java

# Step 2: Run
java RockPaperScissors
```

---

## 📋 Requirements

- **Java JDK 17** or later

> **Don't have Java?** Install it free from [Adoptium (Temurin)](https://adoptium.net) — recommended!

---

## 📁 File Structure

```
Rock, Paper, Scisor Game/
├── RockPaperScissors.java   ← Main source file (all-in-one)
├── compile_and_run.bat      ← Windows launcher script
└── README.md                ← This file
```

After compilation, `.class` files will appear in the same folder.

---

## 🎯 Game Rules

| Player | CPU | Result |
|---|---|---|
| 🪨 Rock | ✂️ Scissors | **You Win** |
| 📄 Paper | 🪨 Rock | **You Win** |
| ✂️ Scissors | 📄 Paper | **You Win** |
| Same | Same | **Tie** |
| Anything else | — | **You Lose** |

---

*Built with ❤️ using pure Java Swing — no external libraries required.*
