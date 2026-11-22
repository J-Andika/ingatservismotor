# 🔧 QA Bug Report & Fix Plan — Ingat Servis Motor

> **Ditulis oleh**: Senior QA Engineer (Code Review)  
> **Tanggal**: 8 Juli 2026  
> **Status**: ❌ APP TIDAK BISA DI-BUILD — Critical Bugs Ditemukan

---

## ⚠️ RINGKASAN EKSEKUTIF

Setelah melakukan review menyeluruh terhadap **seluruh codebase** (19 file Kotlin, 16 file layout XML, 5 resource file, file konfigurasi Gradle), ditemukan **masalah FATAL** yang menyebabkan aplikasi **tidak bisa di-compile/build**. 

Masalah utamanya: **Kode Kotlin dan Layout XML tidak sinkron** — View Binding ID yang dipanggil di Kotlin tidak cocok dengan ID yang ada di layout XML. Ini artinya `ActivityLoginBinding.btnLogin` di Kotlin merujuk ke ID yang tidak ada di layout, sehingga build pasti **GAGAL**.

Selain itu, terdapat file sampah (`new.xml`) yang berisi layout duplikat, fitur yang belum diimplementasikan, dan berbagai masalah keamanan.

---

## 🔴 KATEGORI A: BUG KRITIS (Build Gagal / Crash)

Semua bug di kategori ini **HARUS** diperbaiki agar app bisa di-compile.

---

### BUG-A1: Layout XML dan Kotlin Activity Tidak Sinkron (FATAL)

**Masalah**: ID-ID di layout XML dan yang dipanggil oleh View Binding di Kotlin **berbeda**. Ini menyebabkan build GAGAL.

**Detail Konflik per Screen:**

#### LoginActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.btnLogin` | `@+id/loginButton` |
| `binding.etEmailLogin` | `@+id/emailEditText` |
| `binding.etPasswordLogin` | `@+id/passwordEditText` |
| `binding.tvToRegister` | `@+id/registerTextView` |

#### RegisterActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.btnRegister` | `@+id/registerButton` |
| `binding.etNamaRegister` | `@+id/nameEditText` |
| `binding.etEmailRegister` | `@+id/emailEditText` |
| `binding.etPasswordRegister` | `@+id/passwordEditText` |
| `binding.tvToLogin` | ❌ TIDAK ADA di layout |

#### MainActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.fabAddVehicle` | ❌ TIDAK ADA di layout |
| `binding.tvHeaderTitle` | `@+id/tvHeader` |
| `binding.rvVehicles` | ✅ cocok |
| `binding.bottomNav` | ✅ cocok |
| Bottom nav `R.id.nav_catalog` | Layout punya `R.id.nav_add_vehicle` |

#### VehicleDetailActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.tvDetailName` | `@+id/tvVehicleHeader` |
| `binding.tvDetailType` | ❌ TIDAK ADA |
| `binding.tvDetailYear` | ❌ TIDAK ADA |
| `binding.tvDetailOdometer` | ❌ TIDAK ADA |
| `binding.tvNextService` | ❌ TIDAK ADA |
| `binding.btnInputLog` | ❌ TIDAK ADA |
| `binding.btnHistory` | ❌ TIDAK ADA |
| `binding.btnUpdateOdometer` | ❌ TIDAK ADA |
| `binding.btnFindWorkshop` | ❌ TIDAK ADA |
| `binding.btnDeleteVehicle` | ❌ TIDAK ADA |

#### InputLogActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.etDescription` | ❌ TIDAK ADA (layout punya `etNotes`) |
| `binding.etLogOdometer` | `@+id/etReplacedOdometer` |
| `binding.rvParts` | ❌ TIDAK ADA (layout punya `Spinner`) |
| `binding.btnSaveLog` | ✅ cocok |

#### CatalogActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.rvCatalog` | ❌ TIDAK ADA (layout hanya punya `TextView`) → Kotlin versi disk TIDAK memanggil rvCatalog |

#### ProfileActivity
| Kotlin memanggil | Layout punya |
|---|---|
| `binding.tvProfileName` | `@+id/tvName` |
| `binding.tvProfileEmail` | `@+id/tvEmail` |
| `binding.btnLogout` | ✅ cocok |

**Cara Memperbaiki**:

Pilih SATU versi yang konsisten. Saya sarankan gunakan **Kotlin sebagai "source of truth"** dan perbaiki SEMUA layout XML agar ID-nya cocok dengan apa yang dipanggil di Kotlin.

**File yang harus diubah**:
1. `app/src/main/res/layout/activity_login.xml` — Ganti semua ID agar cocok dengan Kotlin:
   - `loginButton` → `btnLogin`
   - `emailEditText` → `etEmailLogin`
   - `passwordEditText` → `etPasswordLogin`
   - `registerTextView` → `tvToRegister`

2. `app/src/main/res/layout/activity_register.xml` — Ganti semua ID:
   - `registerButton` → `btnRegister`
   - `nameEditText` → `etNamaRegister`
   - `emailEditText` → `etEmailRegister`
   - `passwordEditText` → `etPasswordRegister`
   - Tambahkan `TextView` dengan id `tvToLogin`

3. `app/src/main/res/layout/activity_main.xml` — Rebuild layout sesuai Kotlin:
   - Tambahkan `FloatingActionButton` dengan id `fabAddVehicle`
   - Ganti `tvHeader` → `tvHeaderTitle`
   - Tambahkan `tvHeaderSubtitle`
   - Hapus `llActions` (button Find AHASS dan History)
   - Ganti bottom nav menu item `nav_add_vehicle` → `nav_catalog`

4. `app/src/main/res/layout/activity_vehicle_detail.xml` — Buat ulang layout dengan semua ID yang dibutuhkan Kotlin:
   - `tvDetailName`, `tvDetailType`, `tvDetailYear`, `tvDetailOdometer`
   - `tvNextService`
   - `btnInputLog`, `btnHistory`, `btnUpdateOdometer`, `btnFindWorkshop`, `btnDeleteVehicle`
   - `rvComponentHealth`

5. `app/src/main/res/layout/activity_input_log.xml` — Ganti ID:
   - `etReplacedOdometer` → `etLogOdometer`
   - `etNotes` → `etDescription`
   - Ganti `Spinner` menjadi `RecyclerView` dengan id `rvParts`

6. `app/src/main/res/layout/activity_profile.xml` — Ganti ID:
   - `tvName` → `tvProfileName`
   - `tvEmail` → `tvProfileEmail`
   - Hapus `btnCatalog` (tidak dipakai di Kotlin)

7. `app/src/main/res/layout/activity_catalog.xml` — Tidak perlu diubah jika Kotlin-nya memang kosong

---

### BUG-A2: Data Model Class Tidak Konsisten (FATAL)

**Masalah**: File `VehicleModel.kt` mendefinisikan class `Vehicle` dan `MaintenanceLog`, tapi kode Kotlin lainnya memanggil class/field yang berbeda.

| Kotlin code calls | VehicleModel.kt has |
|---|---|
| `VehicleModel` class | `Vehicle` class |
| `v.nama` | `brand_type` |
| `v.jenisMotor` | tidak ada |
| `v.tahun` | `year` |
| `v.odometerKm` (Double) | `current_odometer` (Long) |
| `v.lastServiceKm` | tidak ada |
| `v.lastServiceDate` | tidak ada |
| `ServiceLog` class | `MaintenanceLog` class |

**Cara Memperbaiki**:

Pilih SATU versi data model. Saya sarankan versi yang lebih lengkap (dari Kotlin Activities):

Ubah file `app/src/main/java/com/JohanesAndika/ingatservismotor/VehicleModel.kt`:

```kotlin
package com.JohanesAndika.ingatservismotor

data class VehicleModel(
    val id: String = "",
    val nama: String = "",
    val jenisMotor: String = "",
    val tahun: String = "",
    val odometerKm: Double = 0.0,
    val lastServiceKm: Double = 0.0,
    val lastServiceDate: String = ""
)

data class ServiceLog(
    val tanggal: String = "",
    val odometerKm: Double = 0.0,
    val deskripsi: String = "",
    val parts: List<String> = emptyList()
)
```

---

### BUG-A3: Firestore Schema Konflik (FATAL)

**Masalah**: Kotlin dan Firestore schema tidak konsisten:

- **MainActivity** query: `db.collection("vehicles").whereEqualTo("userId", uid)` → schema FLAT (vehicles di root collection)
- **AddVehicleActivity & lainnya**: `db.collection("users").document(uid).collection("vehicles")` → schema NESTED (vehicles di sub-collection user)

Dua pendekatan ini **tidak kompatibel**. Data yang ditulis oleh satu Activity tidak bisa dibaca oleh Activity lainnya.

**Cara Memperbaiki**:

Pilih SATU schema. Saya sarankan **schema NESTED** (lebih aman dan scalable):

Path: `users/{uid}/vehicles/{vehicleId}`

Ubah `MainActivity.kt` → ganti query dari:
```kotlin
db.collection("vehicles").whereEqualTo("userId", uid)
```
menjadi:
```kotlin
db.collection("users").document(uid).collection("vehicles")
```

Ubah `ServiceReminderWorker.kt` → juga pakai schema nested, bukan:
```kotlin
db.collection("vehicles").get()
```

Ubah `VehicleDetailActivity.kt` → pastikan semua operasi `maintenance_logs` juga nested:
```kotlin
// SALAH (flat):
db.collection("maintenance_logs").whereEqualTo("vehicleId", vehicleId)
// BENAR (nested):
db.collection("users").document(uid).collection("vehicles").document(vehicleId).collection("serviceLogs")
```

---

### BUG-A4: Adapter Constructor Tidak Cocok (FATAL)

**Masalah**: 
- `VehicleAdapter` di Kotlin menerima `(List<Vehicle>, onUpdateClick, onDetailClick)` — tiga parameter
- Tapi class `VehicleModel` yang seharusnya dipakai adalah `VehicleModel`, bukan `Vehicle`
- `ComponentHealthAdapter` memiliki field `percentage: Int` tapi Kotlin lain memanggilnya dengan `currentKm`, `intervalKm`, `lastReplacedKm`
- `HistoryAdapter` menerima `List<MaintenanceLog>` tapi Kotlin lain memberikan `List<ServiceLog>`
- `PartAdapter` memiliki constructor tanpa parameter (data hardcoded), tapi Kotlin lain memberikan `List<PartItem>`

**Cara Memperbaiki**:

Sesuaikan SEMUA adapter agar konsisten dengan data model yang sudah dipilih di BUG-A2:

1. **VehicleAdapter**: Gunakan `List<VehicleModel>`, hapus callback parameters yang tidak perlu
2. **ComponentHealthAdapter**: Gunakan data class `ComponentHealth(name, currentKm, intervalKm, lastReplacedKm)`
3. **HistoryAdapter**: Gunakan `List<ServiceLog>`
4. **PartAdapter**: Gunakan `List<PartItem>` sebagai constructor parameter

---

### BUG-A5: Missing Resource References (FATAL)

**Masalah**:
- Layout `dialog_add_tracked_component.xml` mereferensi `@array/component_types` — cek apakah ada di `strings.xml`
- Layout `dialog_update_odometer.xml` mereferensi `@string/enter_new_odometer` dan `@string/update`
- Layout `activity_main.xml` mereferensi `@string/find_ahass` dan `@string/history`
- Themes mereferensi style `@style/Widget.BMW.Button`, `@style/Widget.BMW.Card`, `@style/Widget.BMW.FAB`
- Item layout `item_component_health.xml` mereferensi `@+id/tv_calibrating_status` tapi Kotlin memanggilnya sebagai `binding.tvCalibratingStatus`

**Catatan**: Beberapa string ini ADA di versi subagent ketiga tetapi TIDAK ADA di versi subagent kedua — ini menandakan file strings.xml juga tidak konsisten.

**Cara Memperbaiki**:

Pastikan `strings.xml` memiliki SEMUA string yang direferensi:
```xml
<string name="enter_new_odometer">Masukkan odometer baru</string>
<string name="update">Update</string>
<string name="find_ahass">Cari Bengkel</string>
<string name="history">Riwayat</string>
<string-array name="component_types">
    <item>Oli Mesin</item>
    <item>Kampas Rem</item>
    <item>Ban</item>
    <item>Filter Udara</item>
    <item>Busi</item>
    <item>Rantai</item>
    <item>Aki</item>
</string-array>
```

---

### BUG-A6: Theme Parent Konflik (Build Warning/Error)

**Masalah**: 
- `values/themes.xml` memakai parent `Theme.Material3.DayNight.NoActionBar` 
- `values-night/themes.xml` mendefinisikan `Base.Theme.IngatServisMotor` tapi style utama bernama `Theme.IngatServisMotor` — TIDAK ADA inheritance chain dari `Base.Theme` ke `Theme`
- Di `build.gradle.kts`, dependency adalah `material:1.12.0` yang mendukung Material 3, tapi beberapa layout masih referensi `Widget.MaterialComponents.*` (Material 2)

**Cara Memperbaiki**:

1. Hapus file `values-night/themes.xml` atau buat isinya konsisten:
```xml
<resources>
    <style name="Theme.IngatServisMotor" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Night mode overrides jika diperlukan -->
    </style>
</resources>
```

2. Pastikan semua layout memakai style Material 3, bukan Material 2:
   - Ganti `@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox` → `@style/Widget.Material3.TextInputLayout.OutlinedBox`

---

## 🟠 KATEGORI B: BUG FUNGSIONAL (Fitur Rusak/Tidak Bekerja)

Bug ini tidak menyebabkan crash tapi fiturnya **tidak bekerja sesuai harapan**.

---

### BUG-B1: BottomNavigationView Tidak Highlight Item Aktif

**Masalah**: Di `MainActivity.kt`, ketika user kembali dari `CatalogActivity` atau `ProfileActivity`, bottom nav tidak menunjukkan item yang benar karena navigasi menggunakan `startActivity()` tanpa Fragment.

**Cara Memperbaiki**:

Di `MainActivity.onResume()`, reset selected item:
```kotlin
override fun onResume() {
    super.onResume()
    binding.bottomNav.selectedItemId = R.id.nav_home
    loadVehicles()
}
```

---

### BUG-B2: CatalogActivity Kosong (Fitur Belum Implementasi)

**Masalah**: `CatalogActivity.kt` hanya inflate layout dan tidak menampilkan data apapun. Layout `activity_catalog.xml` hanya berisi satu `TextView` bertuliskan "Katalog Komponen".

**Cara Memperbaiki**:

Tambahkan RecyclerView dan data statis komponen Honda di `CatalogActivity`:
```kotlin
// Tambahkan RecyclerView di layout
// Buat daftar PartItem statis untuk katalog Honda
val parts = listOf(
    PartItem("Oli MPX-1", "08232-2MB-K1LZ0", "Rp 55.000"),
    PartItem("Kampas Rem Depan", "06455-KPP-901", "Rp 45.000"),
    PartItem("Busi NGK", "31916-K03-H11", "Rp 15.000"),
    PartItem("Filter Udara", "17210-KYZ-710", "Rp 25.000"),
    PartItem("V-Belt", "23100-KZL-930", "Rp 125.000")
)
```

---

### BUG-B3: SundayWorker Tidak Jalan di Hari Minggu

**Masalah**: `PeriodicWorkRequestBuilder<SundayWorker>(7, TimeUnit.DAYS)` menjadwalkan worker setiap 7 hari **dari waktu enqueue**, bukan setiap hari Minggu. Jika di-enqueue hari Rabu, worker jalan setiap Rabu.

**Cara Memperbaiki**:

Hitung delay hingga hari Minggu berikutnya:
```kotlin
private fun scheduleSundayReminder() {
    val now = Calendar.getInstance()
    val daysUntilSunday = (Calendar.SUNDAY - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
    val initialDelay = if (daysUntilSunday == 0) 7L else daysUntilSunday.toLong()
    
    val req = PeriodicWorkRequestBuilder<SundayWorker>(7, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.DAYS)
        .build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "sunday_check", ExistingPeriodicWorkPolicy.KEEP, req
    )
}
```

---

### BUG-B4: ServiceReminderWorker Mengecek SEMUA Kendaraan (Bukan Milik User)

**Masalah**: Di versi flat schema, `ServiceReminderWorker` query `db.collection("vehicles").get()` — ini mengambil SEMUA kendaraan dari SEMUA user.

**Cara Memperbaiki**:

Filter by current user UID:
```kotlin
val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
val vehicles = db.collection("users").document(uid)
    .collection("vehicles").get().await()
```

---

### BUG-B5: HistoryActivity Sorting Error Potensial

**Masalah**: `orderBy("tanggal")` mengurutkan string tanggal "dd/MM/yyyy" secara leksikografis — ini menghasilkan urutan yang SALAH (misalnya "05/01/2026" muncul sebelum "31/12/2025").

**Cara Memperbaiki**:

Gunakan Firestore `Timestamp` untuk field tanggal, atau simpan tanggal dalam format ISO "yyyy-MM-dd" agar sortable secara leksikografis:
```kotlin
val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
```

---

### BUG-B6: UpdateOdometerDialog Tidak Validasi Input

**Masalah**: User bisa memasukkan odometer baru yang LEBIH KECIL dari odometer lama. Ini merusak kalkulasi health.

**Cara Memperbaiki**:

Tambahkan validasi di `UpdateOdometerDialog`:
```kotlin
val newVal = etOdometer.text.toString().toDoubleOrNull()
if (newVal == null || newVal < currentOdometer) {
    Toast.makeText(context, "Odometer baru harus lebih besar dari ${currentOdometer.toInt()} km", Toast.LENGTH_SHORT).show()
    return@setPositiveButton
}
```

---

### BUG-B7: LocationUtils Crash Jika Google Maps Tidak Terinstall

**Masalah**: `intent.setPackage("com.google.android.apps.maps")` — jika Google Maps tidak terinstall, `startActivity()` akan throw `ActivityNotFoundException`.

**Cara Memperbaiki**:

Tambahkan fallback:
```kotlin
fun openNearbyWorkshops(context: Context) {
    val uri = Uri.parse("geo:0,0?q=bengkel+motor+terdekat")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback ke browser
        val browserIntent = Intent(Intent.ACTION_VIEW, 
            Uri.parse("https://www.google.com/maps/search/bengkel+motor+terdekat"))
        context.startActivity(browserIntent)
    }
}
```

---

### BUG-B8: Tracked Component Tidak Bisa Ditambahkan dari VehicleDetailActivity

**Masalah**: VehicleDetailActivity versi Kotlin (yang ada di disk) tidak memiliki tombol/FAB untuk menambahkan tracked component. Layout XML juga tidak punya FAB. Tapi `dialog_add_tracked_component.xml` sudah ada.

**Cara Memperbaiki**:

Tambahkan FAB di layout `activity_vehicle_detail.xml` dan logic di `VehicleDetailActivity.kt` untuk menampilkan dialog tambah komponen.

---

## 🟡 KATEGORI C: MASALAH KEAMANAN

---

### BUG-C1: Tidak Ada Validasi Password Minimum

**Masalah**: Tidak ada pengecekan panjang password minimum. Firebase membutuhkan minimal 6 karakter, tapi app tidak memvalidasi ini di client side.

**Cara Memperbaiki**:

Di `RegisterActivity.kt`:
```kotlin
if (pass.length < 6) {
    Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
```

---

### BUG-C2: Tidak Ada Email Format Validation

**Masalah**: App hanya cek `email.isEmpty()` tapi tidak validasi format email.

**Cara Memperbaiki**:

```kotlin
if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
    Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
    return@setOnClickListener
}
```

---

### BUG-C3: Tidak Ada Konfirmasi Hapus Kendaraan

**Masalah**: Tombol "Hapus Kendaraan" langsung menghapus tanpa konfirmasi. Data yang terhapus tidak bisa dikembalikan.

**Cara Memperbaiki**:

Tambahkan AlertDialog konfirmasi:
```kotlin
binding.btnDeleteVehicle.setOnClickListener {
    AlertDialog.Builder(this)
        .setTitle("Hapus Kendaraan")
        .setMessage("Yakin ingin menghapus kendaraan ini? Semua data servis juga akan hilang.")
        .setPositiveButton("Hapus") { _, _ ->
            // delete logic
        }
        .setNegativeButton("Batal", null)
        .show()
}
```

---

### BUG-C4: Sub-collection Tidak Terhapus Saat Vehicle Dihapus

**Masalah**: Saat menghapus vehicle, hanya document vehicle yang dihapus. Sub-collection `serviceLogs` dan `trackedComponents` tetap ada sebagai orphan data di Firestore.

**Cara Memperbaiki**:

Hapus sub-collections terlebih dahulu sebelum menghapus vehicle document:
```kotlin
// Hapus serviceLogs
db.collection("users").document(uid).collection("vehicles").document(vehicleId)
    .collection("serviceLogs").get()
    .addOnSuccessListener { docs ->
        for (doc in docs) doc.reference.delete()
    }
// Hapus trackedComponents
db.collection("users").document(uid).collection("vehicles").document(vehicleId)
    .collection("trackedComponents").get()
    .addOnSuccessListener { docs ->
        for (doc in docs) doc.reference.delete()
        // Baru hapus vehicle document
        vehicleRef.delete()
    }
```

---

## 🔵 KATEGORI D: MASALAH UX/UI

---

### BUG-D1: Tidak Ada Empty State

**Masalah**: Jika user baru login dan belum punya kendaraan, `RecyclerView` menampilkan area kosong tanpa pesan apapun.

**Cara Memperbaiki**:

Tambahkan `TextView` "empty state" di layout dan kontrol visibility:
```xml
<TextView android:id="@+id/tvEmptyState"
    android:text="Belum ada kendaraan.\nTap + untuk menambahkan motor Anda."
    android:visibility="gone" />
```
```kotlin
if (vehicleList.isEmpty()) {
    binding.tvEmptyState.visibility = View.VISIBLE
    binding.rvVehicles.visibility = View.GONE
} else {
    binding.tvEmptyState.visibility = View.GONE
    binding.rvVehicles.visibility = View.VISIBLE
}
```

---

### BUG-D2: Tidak Ada Loading Indicator

**Masalah**: Saat data diambil dari Firestore, tidak ada loading indicator. User tidak tahu apakah app sedang loading atau memang kosong.

**Cara Memperbaiki**:

Tambahkan `ProgressBar` di setiap activity yang fetch data:
```kotlin
binding.progressBar.visibility = View.VISIBLE
db.collection(...).get()
    .addOnSuccessListener { 
        binding.progressBar.visibility = View.GONE
        // process data
    }
    .addOnFailureListener {
        binding.progressBar.visibility = View.GONE
        // show error
    }
```

---

### BUG-D3: Tidak Ada Back Navigation

**Masalah**: Activities seperti `VehicleDetailActivity`, `InputLogActivity`, `HistoryActivity`, `AddVehicleActivity` tidak punya Toolbar/ActionBar dengan back button.

**Cara Memperbaiki**:

Tambahkan Toolbar dengan back navigation di setiap secondary activity:
```kotlin
supportActionBar?.setDisplayHomeAsUpEnabled(true)
supportActionBar?.title = "Detail Kendaraan"
```
Atau tambahkan MaterialToolbar di layout.

---

### BUG-D4: File Sampah `new.xml`

**Masalah**: File `app/src/main/new.xml` berisi DUA layout login yang digabung jadi satu file XML. File ini tidak valid (dua root element), bukan bagian dari res/layout, dan kemungkinan besar bekas copy-paste.

**Cara Memperbaiki**:

**HAPUS** file `app/src/main/new.xml`.

---

### BUG-D5: Login Layout Memakai `@color/black` untuk Teks Padahal Background Hitam

**Masalah**: Di `activity_login.xml`, `tvTitleLogin` punya `android:textColor="@color/black"` tapi theme mengatur `android:windowBackground` ke `#000000` (hitam). Hasilnya: **teks hitam di background hitam = tidak terlihat**.

**Cara Memperbaiki**:

Hapus `android:textColor="@color/black"` dari layout login dan register, biarkan theme yang mengatur warna teks (putih di background hitam).

---

## ⚪ KATEGORI E: ARSITEKTUR & CODE QUALITY

---

### BUG-E1: Tidak Mengikuti MVVM yang Direncanakan

**Masalah**: `plan.md` menyebutkan arsitektur MVVM tapi tidak ada `ViewModel`, `Repository`, atau `LiveData`/`StateFlow` di seluruh codebase. Semua logic ada di Activity.

**Cara Memperbaiki** (Jika Diperlukan):

Ini improvement besar. Untuk MVP, cukup buat ViewModel sederhana untuk screen utama:
- `MainViewModel` untuk handle load vehicles
- `VehicleDetailViewModel` untuk handle detail & component health

---

### BUG-E2: Hardcoded Service Interval

**Masalah**: `CalculationUtils.SERVICE_INTERVAL_KM = 3000.0` — semua komponen diasumsikan punya interval servis 3000 km. Padahal oli = 2000 km, kampas rem = 8000 km, ban = 15000 km.

**Cara Memperbaiki**:

Gunakan interval per-komponen dari `trackedComponents` collection di Firestore, bukan hardcoded global.

---

### BUG-E3: Notification Permission Tidak Diminta di Runtime

**Masalah**: `AndroidManifest.xml` deklarasi `POST_NOTIFICATIONS` permission, tapi tidak ada runtime permission request di kode (dibutuhkan Android 13+).

**Cara Memperbaiki**:

Di `MainActivity.onCreate()`:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
```

---

## 📋 PRIORITAS PERBAIKAN (Urutan Kerja)

Kerjakan dalam urutan ini:

### Tahap 1: BUAT APP BISA DI-BUILD (Wajib Selesai Dulu)
1. ✅ Hapus file `app/src/main/new.xml`
2. ✅ Fix `VehicleModel.kt` — konsistenkan data classes (BUG-A2)
3. ✅ Fix SEMUA layout XML agar ID cocok dengan Kotlin (BUG-A1)
4. ✅ Fix adapter constructors (BUG-A4)
5. ✅ Fix string resources (BUG-A5)
6. ✅ Fix theme conflicts (BUG-A6)
7. ✅ Konsistenkan Firestore schema ke nested (BUG-A3)

### Tahap 2: FIX FUNGSIONAL
8. Fix SundayWorker timing (BUG-B3)
9. Fix ServiceReminderWorker scope (BUG-B4)
10. Fix History sorting (BUG-B5)
11. Fix odometer validation (BUG-B6)
12. Fix LocationUtils crash (BUG-B7)
13. Fix BottomNav highlight (BUG-B1)
14. Implementasi CatalogActivity (BUG-B2)
15. Implementasi Add Tracked Component (BUG-B8)

### Tahap 3: FIX KEAMANAN & UX
16. Tambah password validation (BUG-C1)
17. Tambah email validation (BUG-C2)
18. Tambah konfirmasi hapus (BUG-C3)
19. Fix sub-collection deletion (BUG-C4)
20. Tambah empty state (BUG-D1)
21. Tambah loading indicator (BUG-D2)
22. Tambah back navigation (BUG-D3)
23. Fix text color di login (BUG-D5)
24. Tambah notification permission request (BUG-E3)

### Tahap 4: IMPROVEMENT (Opsional)
25. Implement MVVM (BUG-E1)
26. Dynamic service intervals (BUG-E2)

---

## 📌 CATATAN PENTING UNTUK DEVELOPER

1. **JANGAN** jalankan kedua versi kode secara bersamaan — pilih SATU versi (Kotlin atau Layout) sebagai referensi
2. Setelah semua fix di Tahap 1, pastikan **build berhasil** sebelum lanjut ke Tahap 2
3. Test setiap screen secara manual setelah fix:
   - Login → Register → Login kembali
   - Tambah kendaraan → Lihat di daftar
   - Buka detail → Update odometer → Input log servis → Lihat riwayat
   - Hapus kendaraan → Verifikasi terhapus
4. Pastikan **google-services.json** valid dan terhubung ke project Firebase yang benar
