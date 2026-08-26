# KopiKontrol Android Native

Fondasi aplikasi Android murni KopiKontrol menggunakan Kotlin dan Jetpack Compose.
Versi ini tidak memakai WebView untuk menampilkan halaman utama.

## Status migrasi

Versi `2.0.0-alpha01` mencakup:

- Login dan pendaftaran WhatsApp native.
- Login Google melalui browser sistem dan kembali ke aplikasi.
- Penyimpanan sesi akun secara native.
- Onboarding dasar yang menyimpan data ke ruang kerja yang sama.
- Navigasi adaptif untuk handphone dan tablet.
- Ringkasan, daftar bahan, daftar resep, dan profil dari API KopiKontrol.
- Design system native dengan warna, tipografi, card, dan navigasi KopiKontrol.

Modul Transaksi POS, CRUD bahan/resep, scan barcode, printer, nota, pajak,
service charge, dan penyimpanan offline akan dimigrasikan bertahap setelah
fondasi ini stabil.

## Build APK

Setiap push ke branch `main` menjalankan workflow **Build APK**. Unduh artifact
`KopiKontrol-APK` dari halaman Actions. File di dalam artifact bernama
`KopiKontrol-Native-v2.0.0-alpha01.apk`.

## Konfigurasi

- Package ID alpha: `id.kopikontrol.nativealpha` (dapat dipasang berdampingan dengan APK WebView)
- Minimum Android: 7.0 (API 24)
- Backend: `https://kopikontrol-starter.mudrikarzaqi47.chatgpt.site`
- Callback Google: `kopikontrolnative://auth/callback`

APK menggunakan API web yang sudah ada agar akun, sesi, dan data Supabase lama
tetap kompatibel tanpa menaruh secret key Supabase di aplikasi.
