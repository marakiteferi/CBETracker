# CBE Tracker

CBE Tracker is an Android application designed to help users track their financial transactions automatically by reading SMS notifications from the Commercial Bank of Ethiopia (CBE).

## Features

- **Automated Tracking**: Automatically parses transaction SMS from "CBE" and "223".
- **Daily & Monthly Summaries**: View your income, expenses, and net balance for specific days or months.
- **Transaction History**: Detailed list of transactions including the person/entity involved and the amount.
- **Material 3 Design**: Clean and modern user interface built with Jetpack Compose.
- **Privacy Focused**: Processes SMS data locally on your device.

## Screenshots

*(Add screenshots here)*

## How It Works

The app requests `READ_SMS` permission to access the device's inbox. It specifically filters for messages from "CBE" or "223" to identify banking transactions. It then parses these messages to extract:
- Transaction Type (Income/Expense)
- Amount (ETB)
- Counterparty (Person/Service)
- Date

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: Compose State & ViewModel

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/marakiteferi/CBETracker.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on an Android device.
4. Grant SMS reading permissions when prompted to start tracking.

## License

*(Add your license information here, e.g., MIT)*
