package com.dicoding.simplenotif

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.simplenotif.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    companion object {
        // Konstanta string ini dipakai sebagai key saat mengirim & menerima data via Intent extras.
        // Didefinisikan di sini (bukan di MainActivity) agar siapapun yang ingin membuka
        // DetailActivity tahu key yang benar tanpa perlu hardcode string berulang.
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
    }

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Membuat konten meluas ke area system bar agar tampilan edge-to-edge.
        enableEdgeToEdge()

        // ViewBinding menghasilkan referensi view yang null-safe, menggantikan findViewById().
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menambahkan padding agar konten tidak tertimpa system bar (status bar & navigation bar)
        // yang terjadi akibat mode edge-to-edge di atas.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mengambil data yang dikirim oleh pengirim (MainActivity atau notifikasi via PendingIntent).
        // Data ini bisa datang dari dua jalur: tombol "Open Detail" atau ketukan notifikasi.
        val title = intent.getStringExtra(EXTRA_TITLE)
        val message = intent.getStringExtra(EXTRA_MESSAGE)

        // Menampilkan data yang diterima ke UI.
        binding.tvTitle.text = title
        binding.tvMessage.text = message
    }
}