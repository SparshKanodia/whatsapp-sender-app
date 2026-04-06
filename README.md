# 📱 WhatsApp Bulk Messenger (Android)

A Kotlin-based Android application that enables **personalized WhatsApp messaging** using data from Excel files. Built with a focus on **stability, modularity, and safe usage**, this app uses WhatsApp’s native intent system instead of unofficial automation.

---

# 🚀 Overview

This app allows you to:

* Upload an Excel file with contact details
* Generate personalized messages using templates
* Preview messages before sending
* Open WhatsApp chats with pre-filled messages
* Send messages manually in a controlled flow

It is designed primarily for **internal communication use cases** such as:

* Event coordination
* Student notifications (Pareekshaa, TSOT, etc.)
* Team updates

---

# ⚠️ Important Note

This application:

* **Does NOT auto-send messages**
* Uses **WhatsApp Intent (wa.me links)**
* Requires **manual confirmation (tap send)**

This ensures:

* Compliance with platform policies
* Reduced risk of account restrictions
* Long-term usability

---

# 🧠 Core Architecture

```
Excel Upload → Parse Data → Template Engine → Preview → WhatsApp Intent
```

### Architecture Pattern:

* MVVM (Model-View-ViewModel)

### Tech Stack:

* **Language:** Kotlin
* **UI:** Jetpack Compose
* **Navigation:** Jetpack Navigation
* **Excel Parsing:** Apache POI
* **State Management:** ViewModel
* **Persistence (optional):** Room DB

---

# 📂 Project Structure

```
com.example.whatsappbulkmessenger/
│
├── ui/                 # Compose screens (Upload, Preview, Send)
├── data/               # Models, parsing logic
├── domain/             # Business logic (template engine)
├── utils/              # Helpers (encoding, validation)
│
├── MainActivity.kt
└── navigation/
```

---

# 🧩 Features

## 1. 📄 Excel Upload

* Supports `.xlsx` files
* Uses Android Storage Access Framework
* Displays selected file name

---

## 2. 📊 Excel Parsing

* Extracts:

  * Name
  * Phone Number
  * Additional fields
* Handles:

  * Missing values
  * Invalid formats

### Data Model:

```kotlin
data class Contact(
    val name: String,
    val phone: String,
    val extraFields: Map<String, String>
)
```

---

## 3. ✏️ Message Template Engine

Supports dynamic placeholders:

```
Hi {{name}}, your session is on {{date}}
```

### Function:

```kotlin
fun generateMessage(template: String, contact: Contact): String
```

---

## 4. 👀 Message Preview

* Displays:

  * Contact name
  * Phone number
  * Generated message
* Allows:

  * Enabling/disabling recipients

---

## 5. 💬 WhatsApp Integration

Uses:

```
https://wa.me/<number>?text=<encoded_message>
```

### Flow:

1. Opens WhatsApp chat
2. Pre-fills message
3. User taps send manually

---

## 6. ⏱ Controlled Sending Flow

* Sequential messaging
* Random delay between messages (5–20 seconds)
* Countdown timer
* Pause / Resume support

---

## 7. 💾 Session Persistence (Optional)

* Saves:

  * Current progress
  * Contact list
  * Message template
* Restores state on restart

---

## 8. 🛡 Error Handling

Handles:

* Invalid Excel files
* Missing data
* WhatsApp not installed
* Invalid phone numbers

---

# ⚙️ Setup Instructions

## Prerequisites

* Android Studio (latest version recommended)
* Minimum SDK: 24
* Physical Android device (recommended for testing)

---

## Installation

1. Clone the repository:

```
git clone <your-repo-url>
```

2. Open in Android Studio

3. Sync Gradle

4. Run on device

---

## Required Permissions

* Read external storage (for Excel file access)

---

# 🧪 Testing Guidelines

Test the following scenarios:

* Upload valid/invalid Excel files
* Large datasets (500–1000 contacts)
* Missing fields in Excel
* WhatsApp not installed
* App restart during sending

---

# 📈 Future Enhancements

* Multi-template campaigns
* Contact tagging & grouping
* Analytics (sent/skipped tracking)
* Backend sync (Pareekshaa integration)
* Cloud storage for templates

---

# 🧱 Development Workflow (Codex-Based)

Each development step follows:

1. Validate existing code
2. Fix errors and broken references
3. Add new feature
4. Provide summary

This ensures:

* No regressions
* Clean incremental builds
* Stable architecture

---

# 🔐 Safety & Best Practices

* Avoid sending too many messages in a short time
* Use realistic communication patterns
* Always verify recipient consent
* Use for internal or known contacts only

---

# 🤝 Contribution

Feel free to:

* Fork the repo
* Submit pull requests
* Suggest improvements

---

# 📌 Summary

This app is a **controlled, safe, and scalable messaging tool** that bridges Excel-based workflows with WhatsApp communication—without relying on unstable automation or restricted APIs.

---

# 📬 Contact

For queries or improvements, reach out via your development channel or repository issues.
