# KopiKontrol Android Native

Fondasi aplikasi Android murni KopiKontrol menggunakan Kotlin dan Jetpack Compose.
Versi ini tidak memakai WebView untuk menampilkan halaman utama.

## Status migrasi

Versi `2.0.0-alpha02` mencakup:

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
- Input SKU melalui scanner Bluetooth/USB atau input manual.
- Pengaturan printer thermal Bluetooth, ukuran kertas 58/72/80 mm, dan tes cetak ESC/POS.
- Pengaturan nota serta pajak dan service charge yang hanya aktif setelah ada perubahan.
- Pemeriksaan ketersediaan produk berdasarkan stok komponen resep.

Kamera barcode, printer sistem/dot matrix native, sinkronisasi transaksi ke server,
serta CRUD master data akan dilanjutkan pada tahap migrasi berikutnya.

## Build APK

Setiap push ke branch `main` menjalankan workflow **Build APK**. Unduh artifact
`KopiKontrol-Native-v2.0.0-alpha02` dari halaman Actions. File di dalam artifact
bernama `KopiKontrol-Native-v2.0.0-alpha02.apk`.

## Konfigurasi

- Package ID alpha: `id.kopikontrol.nativealpha` (dapat dipasang berdampingan dengan APK WebView)
- Minimum Android: 7.0 (API 24)
- Backend: `https://kopikontrol-starter.mudrikarzaqi47.chatgpt.site`
- Callback Google: `kopikontrolnative://auth/callback`

APK menggunakan API web yang sudah ada agar akun, sesi, dan data Supabase lama
tetap kompatibel tanpa menaruh secret key Supabase di aplikasi.
