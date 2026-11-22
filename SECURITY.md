# Security Policy

## Melaporkan celah

Email ke **johanes.andikaa@gmail.com** — jangan buka issue publik untuk masalah keamanan.

## Catatan tentang Firebase API key

Proyek ini memakai Firebase (Auth + Cloud Firestore). `app/google-services.json`
**tidak di-commit** ke repository ini karena memuat API key (`AIza...`).

Perlu dipahami: API key Android/Firebase adalah **identifier client, bukan rahasia**.
Nilainya selalu bisa diekstrak dari APK yang dipublikasikan (`strings app-release.apk`
atau `aapt dump`). Karena itu keamanan data **tidak** bergantung pada kerahasiaan key,
melainkan pada kontrol berikut (wajib aktif di Google Cloud / Firebase Console):

| Kontrol | Setting yang benar |
|---|---|
| **Application restrictions** (APIs & Services → Credentials → API key) | Android apps → package name `com.JohanesAndika.ingatservismotor` + SHA-1 debug & release |
| **API restrictions** | Hanya API yang dipakai: Identity Toolkit (Firebase Auth), Cloud Firestore, Firebase Installations, FCM, Firebase App Check, Token Service |
| **Firestore Security Rules** | Deny by default; user hanya boleh `users/{request.auth.uid}/**` (terverifikasi: request tanpa auth → `PERMISSION_DENIED`) |
| **Firebase App Check** | Play Integrity untuk Android, lalu enforce di Auth + Firestore |

## Kalau key terlanjur bocor

1. Batasi key yang ada (langkah di atas) — app terpasang tetap jalan.
2. Kalau perlu rotasi: buat key baru (restricted), perbarui `app/google-services.json`,
   perbarui secret CI `GOOGLE_SERVICES_JSON_B64`, rilis versi baru, lalu **hapus** key lama
   (APK versi lama akan berhenti berfungsi).
3. Cek Firebase Console → Authentication → Users untuk akun asing, dan Usage/quotas untuk lonjakan.
