# BayMaxToolsApp

BayMaxToolsApp is an Android application built with **Kotlin** as a central control hub for my IoT devices.  
It enables Bluetooth Low Energy (BLE) communication, forwards WhatsApp and Google Maps navigation notifications to an ESP32 device, and will continue to expand with more features.

## 🚀 Features

- **BLE Scanner**  
  - Scan and connect to nearby BLE devices.  
  - Discover available services and characteristics for communication.

- **WhatsApp Notification Forwarding**  
  - Capture incoming WhatsApp messages via Notification Listener Service.  
  - Forward messages to an ESP32 device over BLE.

- **Google Maps Navigation Forwarding**  
  - Listen to Google Maps turn-by-turn navigation prompts.  
  - Send real-time directions to ESP32 via BLE for TFT display.

## 🛠 Tech Stack

- **Language:** Kotlin  
- **UI Framework:** Jetpack Compose  
- **BLE Library:** Android Bluetooth Low Energy API  
- **Notification Handling:** NotificationListenerService  
- **Minimum SDK:** 26 (Android 8.0 Oreo)

## 📲 Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/digtaalfathir/BayMaxToolsApp.git
   ```
2. Open the project in **Android Studio**.  
3. Sync Gradle dependencies.  
4. Build and install the APK on your Android device.

## 📜 Required Permissions

- `BLUETOOTH`, `BLUETOOTH_ADMIN`
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (Android 12+)
- `ACCESS_FINE_LOCATION`
- `POST_NOTIFICATIONS` (Android 13+)
- `BIND_NOTIFICATION_LISTENER_SERVICE`

## 🔗 ESP32 Compatibility

- **Device Name:** `BayMaxTools`  
- **Service UUID:** `4fafc201-1fb5-459e-8fcc-c5c9c331914b`  
- **Characteristic UUID:** `beb5483e-36e1-4688-b7f5-ea07361b26a8`

## 📅 Roadmap

- [ ] Custom command sending to ESP32 via BLE  
- [ ] Support for more notification sources (Telegram, Email, etc.)  
- [ ] ESP32 device configuration UI  
- [ ] OTA firmware update support  
- [ ] Quick-access BLE widget

