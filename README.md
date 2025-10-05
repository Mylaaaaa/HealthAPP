# 🩺 MyHealth – Personal Health & Wellness Tracker


**MyHealth** is an Android application developed with **Jetpack Compose** and **Health Connect APIs**,  
designed to help users monitor their health data — including **sleep sessions** and **nutrition intake** —  
in a single, unified interface.


---

## 🌟 Features

### 💤 Sleep Session Tracking
- Displays weekly sleep sessions with total duration and stages.
- Expandable cards show details such as **Awake**, **Light**, **Deep**, and **REM** sleep.
- Supports **sample data generation** for demonstration purposes.
- Uses **animated expansion** for a smoother, modern UI experience.

### 🍎 Nutrition Tracking
- Users can record their meals and track **total calorie intake**.
- Designed to support **future Health Connect Nutrition API** integration.
- Prototype system for **personalized meal recommendations** based on health profiles.
- Future updates will include options for chronic condition–based restrictions (e.g. diabetes-safe meals).

### ⚙️ Health Connect Integration
- Handles permission requests to read/write health data.
- Displays a message when permissions are not yet granted.
- Built with **ViewModel + Compose** architecture for reactive UI updates.

---

## 🧩 Tech Stack

| Category | Tools / Libraries |
|-----------|-------------------|
| **Language** | Kotlin |
| **Framework** | Jetpack Compose |
| **Data Source** | Android Health Connect |
| **UI Design** | Material Design 3, Compose Animations |
| **Architecture** | MVVM (ViewModel + State Management) |
| **Build System** | Gradle |

---

## 📱 App Structure

### home
![home1.png](screenshots/home1.png)![img.png](img.png)

![home2.png](screenshots/home2.png)![img_1.png](img_1.png)

### record weight
![recordweight.png](screenshots/recordweight.png)![img_2.png](img_2.png)

---

## 🧠 How It Works

1. When first launched, the app requests **Health Connect permissions**.
2. Once granted, it automatically loads **Sleep** and **Nutrition** data.
3. If data is unavailable, users can simulate records using the “Generate Sleep Data” button.
4. The **Nutrition** screen allows manual meal entry and calorie calculation.
5. Planned future version will provide **AI-based personalized nutrition suggestions**.

---

## 🚀 Future Improvements

- 🔹 Real-time integration with Health Connect Nutrition APIs.
- 🔹 Personalized diet recommendations using AI and user health profiles.
- 🔹 Sleep analytics dashboard (charts & insights).
- 🔹 Cloud data sync and backup.
- 🔹 Firebase authentication for multi-device use.

---



## 👨‍💻 Author

**Zihan Wang**  
IT / Computer Science Student  
James Cook University (JCU), Singapore

📧 *[Optional: add your university email or GitHub link]*  
🌐 GitHub: [Mylaaaaa](https://github.com/Mylaaaaa)

---

## ⚠️ Disclaimer
This application is intended **for academic and educational purposes only**.  
All health data displayed in this project is **simulated**, not real medical information.

---

## 📄 License

MIT License © 2025 Zihan Wang  
You are free to use, modify, and distribute this project with proper attribution.
