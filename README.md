# SimpleNotif 🔔

Aplikasi Android sederhana untuk mempelajari cara kerja **Notification System** di Android modern.

---

## 📱 Fitur

| Fitur | Deskripsi |
|---|---|
| **Send Notification** | Mengirim notifikasi lokal ke notification tray |
| **Open Detail** | Membuka halaman detail langsung dari tombol |
| **Notification Tap** | Mengetuk notifikasi membuka DetailActivity dengan data yang sama |
| **Back-stack yang benar** | Menekan tombol Back dari DetailActivity (dibuka via notifikasi) kembali ke MainActivity, bukan menutup aplikasi |

---

## 🏗️ Struktur Proyek

```
app/src/main/java/com/dicoding/simplenotif/
├── MainActivity.kt       # Layar utama: mengirim notifikasi & navigasi ke detail
└── DetailActivity.kt     # Layar detail: menampilkan judul & pesan dari Intent extras
```

---

## 🧠 Konsep Kunci yang Dipelajari

### 1. Runtime Permission (Android 13+)
Android 13 (API 33) memperkenalkan izin `POST_NOTIFICATIONS` yang harus diminta secara eksplisit kepada pengguna. Di bawah API 33, izin ini otomatis diberikan.

```kotlin
if (Build.VERSION.SDK_INT >= 33) {
    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

> **Kenapa `registerForActivityResult` di luar `onCreate`?**
> Android mewajibkan pendaftaran launcher ini sebelum Activity mencapai state `STARTED`. Jika didaftarkan di dalam `onCreate` setelah `super.onCreate()`, aplikasi akan crash.

---

### 2. Notification Channel (Android 8.0+)
Sejak Android 8.0 (Oreo/API 26), semua notifikasi **wajib** terdaftar pada sebuah channel. Tanpa channel, notifikasi akan diblokir sistem secara diam-diam.

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
    notificationManager.createNotificationChannel(channel)
}
```

> **Aman dipanggil berkali-kali** — jika channel sudah ada, operasi ini diabaikan (no-op).

---

### 3. PendingIntent & Keamanan
`PendingIntent` adalah "tiket" yang diserahkan ke sistem Android agar bisa menjalankan Intent atas nama aplikasi saat notifikasi diketuk.

- **`FLAG_IMMUTABLE`** — Wajib di Android 12+. Mencegah proses lain memodifikasi intent (celah keamanan).
- **`FLAG_UPDATE_CURRENT`** — Jika PendingIntent yang sama sudah ada, perbarui data-nya saja (tidak membuat duplikat).

---

### 4. TaskStackBuilder — Back-Stack yang Benar
Ketika pengguna membuka aplikasi via notifikasi, aplikasi bisa masuk langsung ke `DetailActivity` tanpa `MainActivity` di back-stack. Akibatnya, tombol Back menutup aplikasi sepenuhnya.

`TaskStackBuilder` menyelesaikan masalah ini dengan membaca `android:parentActivityName` di `AndroidManifest.xml` dan membangun urutan back-stack secara otomatis.

```kotlin
// AndroidManifest.xml
<activity
    android:name=".DetailActivity"
    android:parentActivityName=".MainActivity" />
```

```kotlin
// MainActivity.kt
val pendingIntent = TaskStackBuilder.create(this).run {
    addNextIntentWithParentStack(notifDetailIntent) // MainActivity → DetailActivity
    getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
```

---

### 5. ViewBinding
ViewBinding menghasilkan class binding dari file layout secara otomatis, menggantikan `findViewById()`. Keuntungannya:
- **Null-safe** — View yang tidak ada di layout tidak bisa diakses.
- **Type-safe** — Tidak perlu casting manual.
- **Lebih bersih** — Tidak ada pengecekan ID yang salah di compile-time.

---

### 6. Edge-to-Edge UI
`enableEdgeToEdge()` membuat konten aplikasi meluas ke bawah status bar dan navigation bar. Agar konten tidak tertimpa, padding dihitung secara dinamis menggunakan `WindowInsetsCompat`.

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
    insets
}
```

---

### 7. Intent Extras — Pengiriman Data Antar Activity
Data dikirim dari `MainActivity` ke `DetailActivity` menggunakan `Intent.putExtra()` dengan key yang didefinisikan sebagai konstanta di `DetailActivity`.

```kotlin
// Konstanta key ada di DetailActivity agar tidak perlu hardcode string di banyak tempat
const val EXTRA_TITLE = "extra_title"
const val EXTRA_MESSAGE = "extra_message"
```

`DetailActivity` bisa menerima data dari **dua jalur**:
1. Tombol "Open Detail" di `MainActivity`
2. Ketukan notifikasi via `PendingIntent`

---

### 8. NOTIFICATION_ID — Mencegah Duplikasi
Setiap `notify()` yang dipanggil dengan **ID yang sama** akan memperbarui notifikasi yang sudah ada, bukan membuat notifikasi baru. Ini mencegah notification tray penuh dengan notifikasi duplikat jika tombol ditekan berkali-kali.

---

## 🔧 Requirement

| Komponen | Versi |
|---|---|
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 15 (API 35) |
| Bahasa | Kotlin |
| UI Binding | ViewBinding |

---

## 🚀 Cara Menjalankan

1. Clone / buka proyek di Android Studio.
2. Jalankan di emulator atau perangkat fisik (Android 8.0+).
3. Izinkan permission notifikasi saat dialog muncul (Android 13+).
4. Tekan **"Send Notif"** untuk mengirim notifikasi ke tray.
5. Ketuk notifikasi → masuk ke `DetailActivity`.
6. Tekan **Back** → kembali ke `MainActivity` (bukan keluar dari aplikasi).

