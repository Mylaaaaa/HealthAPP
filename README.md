# 🩺 MyHealth – Personal Health & Wellness Tracker

**MyHealth** is an Android application developed with **Jetpack Compose** and **Health Connect APIs**,  
designed to help users monitor and improve their overall wellness — including **exercise**, **sleep**, **nutrition**, **mindfulness**, and **weight tracking** — all in one modern and unified interface.

---

## 👨‍💻 Author
**Zihan Wang**  
🌐 GitHub: [Mylaaaaa](https://github.com/Mylaaaaa)

---

## 🌟 Main Features

### 🔐 Login & Register Module
Provides secure authentication and ensures that only valid users can access the app.

#### 🧩 Key Features
- **Login / Register** options shown at first launch.
- **Email Validation:** Ensures the username is a valid email format.
- **Password Rules:**
  - Must be at least **6 characters long**.
  - Requires **double confirmation**; mismatched entries trigger an error prompt.

💡 *Ensures safe, reliable, and user-friendly access to the MyHealth ecosystem.*

---

### 🏋️ Exercise Session Module
Helps users plan, perform, and review their workouts through four sections: **Plan**, **Workout**, **Courses**, and **State**.

#### 🗓️ Plan Page
- Collects user input (fitness goal, available days, equipment, etc.)
- Generates a **personalized weekly plan** with daily recommendations.  
  💡 *Smart fitness planning based on lifestyle and goals.*

#### 💪 Workout Page
- Displays **daily exercises** with progress tracking and completion rate.  
  💡 *Dynamic progress bar motivates consistency.*

#### 🎥 Courses Page
- Provides recommended video lessons (e.g., Yoga, HIIT).
- Users can **join** or **track** courses they follow.  
  💡 *Built with `LazyColumn` for smooth scrolling and performance.*

#### 📊 State Page
- Shows weekly performance analytics: total time, average duration, and best streak.  
  💡 *Custom Compose charts visualize weekly insights clearly.*

---

### 😴 Sleep Session Module
Tracks and analyzes sleep patterns through **Overview**, **Log**, and **State** pages.

#### 💤 Overview Page
- Displays **today & yesterday’s** deep/light/REM/awake durations and quality.
- Provides **sleep analysis** and **personalized improvement tips**.  
  💡 *Real-time data cards with adaptive visuals.*

#### 📘 Log Page
- Lists all sleep records with timestamps and quality ratings.  
  💡 *Scrollable history for progress comparison.*

#### 📈 State Page
- Shows **7-day stacked bar chart** (Deep / Light / REM / Awake).
- Provides **weekly summaries** with insights.  
  💡 *Color-coded visualization for better understanding.*

---

### 🥗 Nutrition Module
Supports manual food logging, nutrient breakdown, and weekly trend tracking.

#### 🍽️ Overview Page
- Users can manually input food and get automatic calculations of:
  - **Calories**, **Protein**, **Carbs**, and **Fat**
- Accepts optional **chronic condition input** for tailored diet plans.
- Displays a **Recommended Foods** section.  
  💡 *Offers personalized meal guidance and calorie awareness.*

#### 📊 State Page
- Visualizes **weekly calorie intake** and **macro balance**:
  - 7-day calorie trend
  - Macro ratio comparison  
    💡 *Color-coded bar charts make nutrition tracking intuitive.*

---

### 🧘 Mindfulness Module
Improves mental well-being through breathing, mood tracking, and guided meditation.

#### 🌞 Overview Page
- Shows today’s **goal progress**, **motivation messages**, and **quick actions**:
  - **Breathing** (3-min guided)
  - **Mood Check-in** (emoji-based)
- Displays **recent moods** and **guided sessions** (Box Breathing, Body Scan).  
  💡 *Simplified UI encourages daily mindfulness habits.*

#### 📈 State Page
- Provides a deeper look into **weekly mindfulness progress**:
  - 7-day rolling trend
  - Adherence rate and streak count
  - Mood distribution visualization  
    💡 *Uses soft pastel charts for clear, relaxing analytics.*

---

### ⚖️ Record Weight Module
A single-page feature for quick weight input and trend tracking.

#### 🧩 Functionality
- Users enter daily weight manually.
- Displays all records with timestamps.
- Automatically calculates **weekly average**.  
  💡 *Simple design, instant feedback on progress.*

---

### ⚙️ Settings Module
Gives users control over app permissions and appearance.

#### 🧩 Current Features
- **Health Connect Permission:** requested at first launch and editable anytime.

#### 🌙 Planned Features
- **Dark / Light Mode switching**
- **Notification & Theme customization**  
  💡 *Ensures privacy, control, and user comfort.*

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

## 📱 App Overview

### 🏠 Home & Navigation
- Bottom navigation bar provides quick access to all modules.
- Consistent UI layout and smooth transitions.

### 📊 Data Insights
- Real-time, reactive analytics for all modules.
- Includes **rolling 7-day charts**, **streak counts**, and **personalized messages**.

### 🔔 Permissions & Security
- Full Health Connect integration for reading/writing data.
- Clear user consent and data transparency.

---

## 🚀 Future Improvements
- Real-time Health Connect Nutrition integration.
- AI-driven diet and mindfulness recommendations.
- Sleep pattern prediction with ML models.
- Firebase authentication & multi-device sync.
- Cloud data backup and dark mode support.

---

## ⚠️ Disclaimer
This application is intended **for academic and educational purposes only**.  
All displayed health data is **simulated**, not real medical information.
