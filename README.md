Tranzact - Transaction Viewer

Project Overview
Tranzact is a comprehensive Android application that allows users to track and manage their financial transactions securely. The application integrates with a transaction API, implements biometric authentication for enhanced security, and provides an intuitive user interface for transaction management.
Key Features

Secure Authentication: Login system with API integration
Biometric Authentication: Fingerprint login for returning users
Transaction Management: View, search, and filter transactions
Budget Analysis: Visual representation of spending patterns
Offline Mode: Access transaction data without internet connection
Dark Mode: Eye-friendly interface option
Secure Token Handling: Using EncryptedSharedPreferences

App Structure
com.example.transactionviewer/
├── activities/
│   ├── LoginActivity.java
│   ├── MainActivity.java
│   └── SplashActivity.java
├── adapter/
│   ├── CategoryLegendAdapter.java
│   └── TransactionAdapter.java
├── api/
│   ├── ApiClient.java
│   └── ApiService.java
├── database/
│   ├── AppDatabase.java
│   ├── TransactionDao.java
│   ├── TransactionEntity.java
│   ├── TransactionRepository.java
│   ├── UserProfileDao.java
│   ├── UserProfileEntity.java
│   └── UserProfileRepository.java
├── model/
│   ├── CategoryLegendItem.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── Transaction.java
├── security/
│   ├── BiometricHelper.java
│   └── TokenManager.java
├── ui/
│   ├── AnalysisFragment.java
│   ├── ProfileFragment.java
│   └── TransactionFragment.java
└── util/
    └── NetworkUtil.java

Setup Instructions
Prerequisites

Android Studio Arctic Fox (2021.3.1) or newer
JDK 11 or newer
Android SDK with minimum API level 21 (Android 5.0 - Lollipop)
An Android device or emulator with fingerprint sensor capabilities (for biometric authentication)

Clone the Repository
bashgit clone https://github.com/your-username/task1-api-transactions.git
cd task1-api-transactions
API Configuration
The application connects to https://api.prepstripe.com/ for authentication and transaction data. No additional API keys are required.
Database Setup
The Room database is automatically created on first application launch. No additional setup is required.
Build Instructions
Debug Build

Open the project in Android Studio
Connect an Android device or start an emulator
Click on the "Run" button (▶️) or press Shift+F10
Select the target device and click "OK"

Release Build
To generate a signed APK:

In Android Studio, go to Build → Generate Signed Bundle/APK
Select APK
Create a new keystore or use an existing one:

If creating new, fill out the required fields and remember your password
If using existing, browse to your keystore location and enter the password


Click Next
Select "release" build variant
Check both V1 and V2 Signature Versions
Click Finish

The signed APK will be generated in app/release/app-release.apk
Creating GitHub Release

Create a tag for your release:
bashgit tag -a v1.0.0 -m "Version 1.0.0"
git push origin v1.0.0

Go to your GitHub repository
Click on "Releases" in the right sidebar
Click "Create a new release"
Select the tag you just created
Add a title and description for your release
Drag and drop your APK file (app/release/app-release.apk)
Click "Publish release"

Usage Guide
First Launch

Upon first launch, you will see the splash screen with the Tranzact logo
You will be directed to the login screen
Enter your username and password
Tap "LOGIN" to authenticate

Subsequent Launches

The app will prompt for biometric authentication
Use your fingerprint to unlock the application
Alternatively, you can switch back to password login

Features

Transactions Tab: View all your transactions

Search transactions using the search bar
Filter transactions by amount or date
View transaction details


Analysis Tab: Visualize your spending

Set monthly budget
Monitor expenses by category
View spending distribution in pie chart


Profile Tab: Manage your settings

Toggle Dark Mode
Enable/Disable Biometric Lock
Sign out



Bonus Features Implemented

Dark Mode: Toggle between light and dark themes from the Profile screen
Offline Mode: Transactions are cached using Room Database and available offline with a banner notification
Search/Filter Functionality: Search transactions by name and filter by various criteria:

Latest first
Oldest first
Amount (high to low)
Amount (low to high)


Budget Analysis: Visual representation of spending with interactive charts
Profile Picture: User can set and view their profile image

Technical Details
Security Implementation

Token Storage: Secure token storage using EncryptedSharedPreferences
Biometric Authentication: Implemented using BiometricPrompt API
Network Security: HTTPS connections with certificate pinning

Libraries Used

Retrofit: For API integration
Room: For local database management
MPAndroidChart: For data visualization
Glide: For image loading and caching
Material Components: For UI elements

Troubleshooting
Common Issues

Biometric Authentication Not Working: Ensure your device has fingerprint capabilities and you have enrolled at least one fingerprint
Offline Mode Not Syncing: Check your internet connection and try refreshing the transactions screen
API Connection Errors: Verify your internet connection and try again

License
This project is licensed under the MIT License - see the LICENSE file for details.
Contact
For questions or support, please contact your-email@example.com
