# Project Plan: Ingat Servis Motor

## 1. Executive Summary
**Ingat Servis Motor** adalah aplikasi *mobile* berbasis Android yang dirancang dengan pendekatan *User-Centered Design* (UCD) untuk membantu pemilik sepeda motor Honda (khususnya kelas di bawah 250cc) dalam merawat kendaraan mereka. Aplikasi ini mengatasi masalah kelalaian perawatan rutin dengan melacak umur komponen *fast-moving* (seperti oli, kampas rem, ban) dan memberikan notifikasi proaktif. Melalui pencatatan riwayat penggantian komponen dan estimasi jarak tempuh, aplikasi ini memastikan kendaraan selalu dalam kondisi prima, mencegah kerusakan berat, dan memperpanjang usia pakai komponen.

## 2. Tech Stack & Architecture
*   **Platform:** Native Android
*   **Bahasa Pemrograman:** Kotlin
*   **Arsitektur Aplikasi:** MVVM (Model-View-ViewModel) dengan Clean Architecture principles.
*   **Backend & Database:** 
    *   Firebase Authentication (Email/Password & Google Sign-In)
    *   Cloud Firestore (NoSQL Database untuk sinkronisasi *real-time* dan *offline support*)
*   **UI/UX:** Material Design 3 (Material You)
*   **Background Processing (Notifikasi):** Android `WorkManager` (untuk penjadwalan notifikasi mingguan dan kalkulasi estimasi harian).
*   **Dependency Injection:** Hilt / Dagger (Opsional, sangat disarankan untuk skalabilitas).

## 3. Core Features & Roadmap

### Phase 1: Minimum Viable Product (MVP)
Fokus pada fungsi inti pelacakan dan pengingat dasar.
*   **Autentikasi Pengguna:** Login dan Register menggunakan Firebase Auth.
*   **Pencatatan Riwayat (Manual Input):** Form untuk mencatat penggantian oli dan komponen lain, beserta angka *odometer* saat penggantian dilakukan.
*   **Notifikasi Pembaruan Odometer (Weekly):** Pengingat otomatis yang berjalan setiap hari Minggu untuk meminta pengguna memperbarui angka *odometer* terkini (pemakaian mingguan).
*   **Smart Service Reminder:** Sistem notifikasi otomatis yang mengingatkan jadwal servis/ganti komponen berdasarkan:
    *   Tanggal terakhir komponen diganti.
    *   Kalkulasi estimasi jarak tempuh harian (berdasarkan selisih *odometer* mingguan).

### Phase 2: Feature Expansion
*   **Multi-Vehicle Support:** Pengguna dapat menambahkan lebih dari satu motor di bawah 250cc dalam satu akun.
*   **Katalog Komponen Orisinal:** Menampilkan daftar *part number* (HGP - Honda Genuine Parts) dan estimasi harga untuk motor pengguna.
*   **Visualisasi Data:** Grafik penggunaan motor dan riwayat pengeluaran servis.

### Phase 3: Advanced Integrations
*   **Predictive Maintenance:** Menggunakan *machine learning* sederhana untuk memprediksi keausan komponen berdasarkan gaya berkendara.
*   **Integrasi Bengkel Resmi:** Pencarian lokasi AHASS terdekat menggunakan Google Maps API.

## 4. App Architecture & Folder Structure
Direkomendasikan menggunakan struktur folder berdasarkan fitur (*Feature-based modularization*) atau berdasarkan lapisan arsitektur. Berikut adalah draf struktur paket (*package structure*):

```text
com.project.ingatservismotor
│
├── core/                   # Kelas dasar, utils, dan extensions
│   ├── utils/              # Helper untuk kalkulasi tanggal, estimasi jarak
│   └── workers/            # Kelas WorkManager untuk notifikasi mingguan (SundayWorker)
│
├── data/                   # Lapisan Data (Repository & Model)
│   ├── model/              # Data Class Kotlin (User, Vehicle, ServiceLog)
│   ├── repository/         # Logika pengambilan data dari Firestore
│   └── remote/             # Konfigurasi Firebase Firestore & Auth
│
├── di/                     # Dependency Injection modules (Hilt)
│
├── ui/                     # Lapisan Presentasi (Activity, Fragment/Compose, ViewModel)
│   ├── auth/               # Layar Login & Register
│   ├── dashboard/          # Layar Utama (Status komponen & Ringkasan)
│   ├── inputlog/           # Layar Form pencatatan servis & update odometer
│   └── profile/            # Layar Pengaturan & Kendaraan
│
└── App.kt                  # Base Application class