package com.dicoding.simplenotif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.simplenotif.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Launcher ini didaftarkan lebih awal (sebelum onCreate) karena Android mengharuskan
    // registerForActivityResult() dipanggil sebelum Activity mencapai state STARTED.
    // Hasilnya berupa boolean: true = izin diberikan, false = izin ditolak.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Membuat konten aplikasi meluas hingga ke area system bar (status bar & navigation bar)
        // agar tampilan lebih modern dan mengisi layar penuh (edge-to-edge).
        enableEdgeToEdge()

        // ViewBinding dipakai agar tidak perlu findViewById() berulang kali,
        // sehingga akses ke view lebih aman (null-safe) dan lebih efisien.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Karena edge-to-edge aktif, konten bisa tertimpa oleh system bar.
        // Listener ini memberi padding otomatis sesuai tinggi system bar
        // sehingga konten tetap terlihat dan tidak terpotong.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Android 13 (API 33) ke atas memerlukan izin POST_NOTIFICATIONS secara eksplisit dari pengguna.
        // Di bawah API 33, izin ini tidak diperlukan karena sudah otomatis diberikan.
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val title = getString(R.string.notification_title)
        val message = getString(R.string.notification_message)

        binding.btnSendNotification.setOnClickListener {
            sendNotification(title, message)
        }

        binding.btnOpenDetail.setOnClickListener {
            // Membuka DetailActivity langsung dari tombol (bukan dari notifikasi),
            // sehingga data title & message dikirim via Intent extras.
            val detailIntent = Intent(this@MainActivity, DetailActivity::class.java)
            detailIntent.putExtra(DetailActivity.EXTRA_TITLE, title)
            detailIntent.putExtra(DetailActivity.EXTRA_MESSAGE, message)
            startActivity(detailIntent)
        }
    }

    private fun sendNotification(title: String, message: String) {
//        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://dicoding.com"))
//        val pendingIntent = PendingIntent.getActivity(
//            this,
//            0,
//            intent,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
//        )

        // Intent ini akan dijalankan ketika pengguna mengetuk notifikasi,
        // membawa data title & message agar DetailActivity bisa menampilkannya.
        val notifDetailIntent = Intent(this, DetailActivity::class.java)
        notifDetailIntent.putExtra(DetailActivity.EXTRA_TITLE, title)
        notifDetailIntent.putExtra(DetailActivity.EXTRA_MESSAGE, message)

        // TaskStackBuilder digunakan agar back-stack navigasi terbentuk dengan benar.
        // Tanpa ini, jika pengguna menekan tombol Back dari DetailActivity yang dibuka
        // lewat notifikasi, aplikasi langsung tertutup. Dengan TaskStackBuilder,
        // MainActivity otomatis ditambahkan ke back-stack sehingga Back kembali ke sana.
        val pendingIntent = TaskStackBuilder.create(this).run {
            // addNextIntentWithParentStack() membaca android:parentActivityName di Manifest
            // untuk membangun urutan back-stack secara otomatis.
            addNextIntentWithParentStack(notifDetailIntent)

            // FLAG_UPDATE_CURRENT: memperbarui data intent jika PendingIntent sudah ada sebelumnya.
            // FLAG_IMMUTABLE: wajib di Android 12+ untuk mencegah intent dimodifikasi oleh proses lain (keamanan).
            getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            //            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//            } else {
//                getPendingIntent(NOTIFICATION_ID, PendingIntent.FLAG_UPDATE_CURRENT)
//            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationCompat.Builder dipakai (bukan NotificationManager.Builder) supaya
        // kode tetap kompatibel dengan versi Android lama tanpa perlu cek versi manual.
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.baseline_notifications_active_24) // Ikon wajib ada, tanpa ini notifikasi tidak muncul
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Prioritas menentukan seberapa mengganggu notifikasi (apakah muncul sebagai heads-up atau tidak)
            .setSubText(getString(R.string.notification_subtext)) // Teks kecil di atas title, biasanya untuk kategori/sumber notifikasi
            .setContentIntent(pendingIntent) // Aksi yang dijalankan saat notifikasi ditekan
            .setAutoCancel(true) // Notifikasi otomatis hilang dari tray setelah pengguna mengetuknya

        // NotificationChannel wajib dibuat di Android 8.0 (Oreo/API 26) ke atas.
        // Sistem Android memblokir semua notifikasi yang tidak punya channel terdaftar.
        // Membuat channel yang sama berkali-kali aman (operasi no-op jika sudah ada).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            )
            builder.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = builder.build()

        // NOTIFICATION_ID dipakai sebagai identifier unik notifikasi ini.
        // Jika notify() dipanggil lagi dengan ID yang sama, notifikasi lama akan diperbarui,
        // bukan membuat notifikasi baru — mencegah duplikasi di notification tray.
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1       // ID unik untuk notifikasi ini
        private const val CHANNEL_ID = "channel_01" // ID channel harus unik per aplikasi
        private const val CHANNEL_NAME = "dicoding channel" // Nama channel yang terlihat oleh pengguna di pengaturan notifikasi
    }
}