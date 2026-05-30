# Daiet — AI-Powered diet app

**Daiet** is a modern, minimalist Android application built with Jetpack Compose, designed to simplify nutrition tracking through the power of Artificial Intelligence. Unlike traditional counters that require tedious manual database searches, Daiet allows users to log their meals naturally and intelligently.

## Project Overview

The core philosophy of Daiet is to act as a personal digital dietitian in your pocket. It removes the friction of calorie counting by allowing users to describe their meals in plain English or simply scan a barcode. The integrated AI (powered by Llama 3.3 via Groq) handles the complex task of identifying ingredients and estimating portions to provide an instant nutritional breakdown.

Beyond logging, the application serves as a culinary inspiration hub, featuring a vast library of global recipes. Users can explore various cuisines, view detailed preparation steps, and instantly log specific portions of these dishes. The app automatically calculates the specific caloric and macronutrient impact based on the portion size described by the user.

## Key Capabilities

- **Intelligent Natural Language Processing**: Log meals by describing them (e.g., "A bowl of Greek yogurt with five strawberries and a tablespoon of honey") and get instant nutritional data.
- **Recipe-Based Logging**: Access a large database of recipes with step-by-step instructions. Log your consumption by portion (e.g., "half of the recipe") and let AI do the math.
- **Instant Barcode Recognition**: Quick product logging using the OpenFoodFacts database via high-performance scanning.
- **Personalized Nutrition Goals**: Set and monitor daily targets for calories, protein, carbohydrates, fats, and fiber with intuitive progress visualization.
- **Secure Cloud Sync**: User accounts and data persistence powered by Firebase Authentication.
- **Modern Design**: A fully responsive UI built with Material Design 3, including seamless support for Light, Dark, and System-adaptive themes.

## Getting Started

### Prerequisites

- **Android Studio Iguana** (2023.2.1) or newer.
- A **Firebase Project** with Email/Password authentication enabled.
- A **Groq API Key** for AI nutritional processing (available at [Groq Cloud](https://console.groq.com/keys)).

### Installation & Setup

1. **Clone the repository**:
2. **Configure Firebase**:
   - Download the `google-services.json` file from your Firebase console.
   - Place it in the `app/` directory of the project.
3. **Build & Run**:
   - Open the project in Android Studio, sync Gradle, and run the app on an Android 8.0+ device.
4. Configure Groq API key.

## Technical Stack

- **Kotlin & Jetpack Compose**: UI and core logic.
- **Firebase Auth**: Secure user management.
- **Room Persistence**: Local SQLite storage for meal history.
- **OkHttp & Gson**: Reliable network communication.
- **Groq AI (Llama 3.3)**: Advanced nutritional analysis.
- **TheMealDB API**: Source for global recipe discovery.
- **Coil**: Optimized asynchronous image loading.
- **CameraX & ML Kit**: High-speed barcode scanning.
