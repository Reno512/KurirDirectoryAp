package com.latihan.kurirdirectoryapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // 1. Deklarasi Komponen Tampilan
    private lateinit var editTextSearchName: EditText
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var textViewErrorMessage: TextView
    private lateinit var textViewEmptyMessage: TextView
    private lateinit var recyclerViewUsers: RecyclerView

    // Variabel Penampung Data
    private lateinit var userAdapter: UserAdapter
    private val daftarPenggunaAsli = mutableListOf<UserResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Inisialisasi Elemen Antarmuka
        editTextSearchName = findViewById(R.id.editTextSearchName)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        textViewErrorMessage = findViewById(R.id.textViewErrorMessage)
        textViewEmptyMessage = findViewById(R.id.textViewEmptyMessage)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)

        // Konfigurasi RecyclerView dengan LinearLayoutManager vertikal
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList())
        recyclerViewUsers.adapter = userAdapter

        // 3. Mengambil Data Langsung Saat Aplikasi Dibuka
        muatDataPengguna()

        // 4. Fitur Pencarian Nama & Perusahaan Real-Time (Live Search Filter)
        editTextSearchName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val kataKunci = s.toString().trim()
                filterNamaPengguna(kataKunci)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun muatDataPengguna() {
        progressBarLoading.visibility = View.VISIBLE
        textViewErrorMessage.visibility = View.GONE
        textViewEmptyMessage.visibility = View.GONE

        // Eksekusi Panggilan Jaringan Asinkron pada IO Thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllUsers()

                // Kembali ke Main Thread untuk Mengupdate Tampilan UI
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body() != null) {
                        val dataDiterima = response.body()!!
                        daftarPenggunaAsli.clear()
                        daftarPenggunaAsli.addAll(dataDiterima)

                        // Tampilkan Seluruh Data ke RecyclerView
                        userAdapter.perbaruiDaftar(daftarPenggunaAsli)
                    } else {
                        textViewErrorMessage.text = "Gagal memuat data dari server (HTTP ${response.code()})"
                        textViewErrorMessage.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE
                    textViewErrorMessage.text = "Koneksi internet bermasalah: ${e.localizedMessage ?: "Gagal terhubung"}"
                    textViewErrorMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun filterNamaPengguna(kataKunci: String) {
        val daftarTersaring = if (kataKunci.isEmpty()) {
            daftarPenggunaAsli
        } else {
            // TUGAS 1: Filter berdasarkan NAMA atau NAMA PERUSAHAAN
            daftarPenggunaAsli.filter { pengguna ->
                pengguna.name.contains(kataKunci, ignoreCase = true) ||
                        pengguna.company.companyName.contains(kataKunci, ignoreCase = true)
            }
        }

        userAdapter.perbaruiDaftar(daftarTersaring)

        // TUGAS 2: Tampilkan pesan ketika hasil pencarian kosong
        if (daftarTersaring.isEmpty()) {
            textViewEmptyMessage.visibility = View.VISIBLE
            recyclerViewUsers.visibility = View.GONE
        } else {
            textViewEmptyMessage.visibility = View.GONE
            recyclerViewUsers.visibility = View.VISIBLE
        }
    }
}
