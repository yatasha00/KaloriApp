package com.example.kaloriapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, AnaSayfa::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val etAd = findViewById<EditText>(R.id.etAd)
        val etSoyad = findViewById<EditText>(R.id.etSoyad)
        val etEposta = findViewById<EditText>(R.id.etEposta)
        val etKullanici = findViewById<EditText>(R.id.etKullanici)
        val etSifre = findViewById<EditText>(R.id.etSifre)
        val etSifreTekrar = findViewById<EditText>(R.id.etSifreTekrar)
        val btnKayit = findViewById<Button>(R.id.btnKayit)

        val ref = FirebaseDatabase.getInstance().getReference("Kullanicilar")

        btnKayit.setOnClickListener {
            val ad = etAd.text.toString().trim()
            val soyad = etSoyad.text.toString().trim()
            val eposta = etEposta.text.toString().trim()
            val kullaniciAdi = etKullanici.text.toString().trim()
            val sifre = etSifre.text.toString().trim()
            val sifreTekrar = etSifreTekrar.text.toString().trim()

            if (ad.isEmpty() || soyad.isEmpty() || eposta.isEmpty() || kullaniciAdi.isEmpty() || sifre.isEmpty() || sifreTekrar.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ad.all { it.isLetter() || it.isWhitespace() } || !soyad.all { it.isLetter() || it.isWhitespace() }) {
                Toast.makeText(this, "Name must contain only letters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(eposta).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sifre.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sifre != sifreTekrar) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(eposta, sifre)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val kullaniciId = auth.currentUser?.uid
                        val yeniKullanici = KullaniciModel(ad, soyad, eposta, kullaniciAdi)

                        if (kullaniciId != null) {
                            FirebaseDatabase.getInstance().getReference("Kullanicilar").child(kullaniciId).setValue(yeniKullanici)
                                .addOnSuccessListener {
                                    // YENİ HESAPLAMA SAYFASINA GÖNDER
                                    startActivity(Intent(this, GoalCalculatorActivity::class.java))
                                    finish()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Sign up failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        val btnGiris = findViewById<Button>(R.id.btnGiris)
        btnGiris.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun showOnboardingDialog(ad: String, soyad: String, eposta: String, kullaniciAdi: String) {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }

        val etAge = android.widget.EditText(this).apply { hint = "Age (e.g. 21)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etHeight = android.widget.EditText(this).apply { hint = "Height in cm (e.g. 180)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val etWeight = android.widget.EditText(this).apply { hint = "Weight in kg (e.g. 75)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER }

        layout.addView(etAge)
        layout.addView(etHeight)
        layout.addView(etWeight)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Welcome to KaloriApp!")
            .setMessage("Let's calculate your ideal daily calorie goal. Please enter your metrics:")
            .setCancelable(false) // Kullanıcı boş geçemesin diye dışarı tıklamayı kapattık
            .setView(layout)
            .setPositiveButton("Calculate & Start") { _, _ ->
                val age = etAge.text.toString().toIntOrNull() ?: 0
                val height = etHeight.text.toString().toIntOrNull() ?: 0
                val weight = etWeight.text.toString().toIntOrNull() ?: 0

                // Varsayılan hedef 2000, ama doğru girdiyse formül çalışacak
                var dailyGoal = 2000
                if (age > 0 && height > 0 && weight > 0) {
                    val bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5
                    dailyGoal = (bmr * 1.375).toInt()
                } else {
                    Toast.makeText(this, "Invalid inputs, setting default to 2000 kcal.", Toast.LENGTH_SHORT).show()
                }

                // Kullanıcıyı Firebase'e ve Hafızaya Kaydetme İşlemi
                val kullaniciId = auth.currentUser?.uid
                if (kullaniciId != null) {
                    val yeniKullanici = KullaniciModel(ad, soyad, eposta, kullaniciAdi)

                    // 1. Firebase'e ad-soyad yaz
                    FirebaseDatabase.getInstance().getReference("Kullanicilar").child(kullaniciId).setValue(yeniKullanici)
                        .addOnSuccessListener {
                            // 2. Hafızaya (SharedPreferences) özel kalori hedefini kaydet
                            val hafiza = getSharedPreferences("KaloriApp_$kullaniciId", android.content.Context.MODE_PRIVATE)
                            hafiza.edit().putInt("HedefKalori", dailyGoal).apply()

                            // 3. Ana Sayfaya Fırlat
                            Toast.makeText(this, "Goal set to $dailyGoal kcal!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AnaSayfa::class.java))
                            finish()
                        }
                }
            }
            .show()
    }
}