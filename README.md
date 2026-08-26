# KopiKontrol Android Native

Fondasi aplikasi Android murni KopiKontrol menggunakan Kotlin dan Jetpack Compose.
Versi ini tidak memakai WebView untuk menampilkan halaman utama.

## Status migrasi

Versi `2.0.0-alpha04` mencakup:

- Login dan pendaftaran WhatsApp native.
- Login Google melalui browser sistem dan kembali ke aplikasi.
- Penyimpanan sesi akun secara native.
- Onboarding dasar yang menyimpan data ke ruang kerja yang sama.
- Navigasi adaptif untuk handphone dan tablet.
- Ringkasan, daftar bahan, daftar resep, dan profil dari API KopiKontrol.
- Design system native dengan warna, tipografi, card, dan navigasi KopiKontrol.
- POS adaptif: katalog, filter kategori, badge jumlah, pesanan, dan pembayaran terpisah.
- Validasi metode pembayaran dan uang diterima, nominal cepat khusus tunai, diskon, pajak, serta service charge.
- Riwayat transaksi lokal dan popup transaksi berhasil.
- Scan barcode langsung dari kamera, scanner Bluetooth/USB, atau input manual.
- Barcode yang belum dikenal dapat ditautkan ke produk dan disimpan lokal di perangkat.
- Pengaturan printer thermal Bluetooth, ukuran kertas 58/72/80 mm, dan tes cetak ESC/POS.
- Permintaan izin Perangkat di sekitar sebelum hubungkan, tes cetak, dan cetak struk pada Android 12+.
- Perbaikan error `BLUETOOTH_SCAN`: koneksi printer yang sudah dipasangkan tidak lagi memanggil discovery.
- Status printer terhubung/tidak terhubung berdasarkan hasil koneksi cetak terakhir.
- Cetak dot matrix/printer sistem melalui dialog Print Service Android dengan nota 72 mm.
- Cetak ulang transaksi langsung dari riwayat.
- Sidebar tablet dapat dibuka atau ditutup melalui tombol burger di header.
- Profil adaptif berisi identitas kedai, pemilik, ringkasan usaha, dan status langganan.
- Pengaturan nota serta pajak dan service charge yang hanya aktif setelah ada perubahan.
- Pemeriksaan ketersediaan produk berdasarkan stok komponen resep.

Sinkronisasi transaksi ke server serta CRUD master data native akan dilanjutkan
pada tahap migrasi berikutnya.

## Build APK

Setiap push ke branch `main` menjalankan workflow **Build APK**. Unduh artifact
`KopiKontrol-Native-v2.0.0-alpha04` dari halaman Actions. File di dalam artifact
bernama `KopiKontrol-Native-v2.0.0-alpha04.apk`.

## Konfigurasi

- Package ID alpha: `id.kopikontrol.nativealpha` (dapat dipasang berdampingan dengan APK WebView)
- Minimum Android: 7.0 (API 24)
- Backend: `https://kopikontrol-starter.mudrikarzaqi47.chatgpt.site`
- Callback Google: `kopikontrolnative://auth/callback`

APK menggunakan API web yang sudah ada agar akun, sesi, dan data Supabase lama
tetap kompatibel tanpa menaruh secret key Supabase di aplikasi.
