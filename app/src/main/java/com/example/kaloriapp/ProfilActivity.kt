package com.example.kaloriapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfilActivity : AppCompatActivity() {

    private lateinit var tvProfilBuyukHarf: TextView
    private lateinit var tvProfilFullName: TextView
    private lateinit var tvProfilUsername: TextView
    private lateinit var tvProfilEmail: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        tvProfilBuyukHarf = findViewById(R.id.tvProfilBuyukHarf)
        tvProfilFullName = findViewById(R.id.tvProfilFullName)
        tvProfilUsername = findViewById(R.id.tvProfilUsername)
        tvProfilEmail = findViewById(R.id.tvProfilEmail)

        fetchUserData()

        // NAVBAR KONTROLLERİ
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationViewProfil)
        bottomNav.selectedItemId = R.id.nav_profil

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ana_sayfa -> {
                    startActivity(Intent(this, AnaSayfa::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_haftalik -> {
                    startActivity(Intent(this, HaftalikActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profil -> true
                else -> false
            }
        }

        val btnCikisYapProfil = findViewById<TextView>(R.id.btnCikisYapProfil)
        btnCikisYapProfil.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // YENİ MODERN MENÜYÜ ÇAĞIRAN KISIM
        val cardCalculateGoal = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardCalculateGoal)
        cardCalculateGoal.setOnClickListener {
            showUpdateGoalMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationViewProfil)
        bottomNav.selectedItemId = R.id.nav_profil
    }

    private fun fetchUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val uid = user.uid

            tvProfilEmail.text = user.email ?: "No Email"

            FirebaseDatabase.getInstance().getReference("Kullanicilar").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val ad = snapshot.child("ad").value?.toString() ?: ""
                        val soyad = snapshot.child("soyad").value?.toString() ?: ""
                        val username = snapshot.child("kullaniciAdi").value?.toString() ?: "Unknown"

                        val fullName = "$ad $soyad".trim()

                        if (fullName.isNotEmpty()) {
                            tvProfilFullName.text = fullName
                            tvProfilBuyukHarf.text = fullName.substring(0, 1).uppercase()
                        } else {
                            tvProfilFullName.text = "User"
                        }

                        tvProfilUsername.text = "@$username"
                    } else {
                        Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show()
                }
        } else {
            tvProfilFullName.text = "Guest"
            tvProfilEmail.text = "Not logged in"
        }
    }

    // --- MODERN BOTTOM SHEET TASARIMLARI ---

    private fun showUpdateGoalMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        val title = TextView(this).apply { text = "Update Goal"; textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(android.graphics.Color.parseColor("#212529")); setPadding(0, 0, 0, 32) }
        layout.addView(title)

        val btnWizard = Button(this).apply {
            text = "✨ Use Calorie Wizard"
            isAllCaps = false; textSize = 16f; setTextColor(android.graphics.Color.WHITE); setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150).apply { setMargins(0, 0, 0, 24) }
            setOnClickListener { bottomSheetDialog.dismiss(); startActivity(Intent(this@ProfilActivity, GoalCalculatorActivity::class.java)) }
        }
        layout.addView(btnWizard)

        val btnManual = Button(this).apply {
            text = "✍️ Set Manually"
            isAllCaps = false; textSize = 16f; setTextColor(android.graphics.Color.parseColor("#2E7D32")); setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
            setOnClickListener { bottomSheetDialog.dismiss(); showManualGoalDialog() }
        }
        layout.addView(btnManual)

        bottomSheetDialog.setContentView(layout); bottomSheetDialog.show()
    }

    private fun showManualGoalDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        val title = TextView(this).apply { text = "Manual Entry"; textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(android.graphics.Color.parseColor("#212529")); setPadding(0, 0, 0, 16) }
        val subtitle = TextView(this).apply { text = "Enter your custom daily calorie goal:"; textSize = 14f; setTextColor(android.graphics.Color.parseColor("#6C757D")); setPadding(0, 0, 0, 32) }

        val inputBg = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#F8F9FA")); cornerRadius = 16f; setStroke(2, android.graphics.Color.parseColor("#E9ECEF")) }

        val input = EditText(this).apply {
            hint = "e.g. 2200"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setPadding(40, 40, 40, 40); background = inputBg
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
        }

        val btnSave = Button(this).apply {
            text = "SAVE GOAL"
            setTextColor(android.graphics.Color.WHITE); setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
            setOnClickListener {
                val newGoal = input.text.toString().toIntOrNull() ?: 2000
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "Guest"
                getSharedPreferences("KaloriApp_$uid", Context.MODE_PRIVATE).edit().putInt("HedefKalori", newGoal).apply()
                Toast.makeText(this@ProfilActivity, "Goal updated to $newGoal kcal", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }
        }

        layout.addView(title); layout.addView(subtitle); layout.addView(input); layout.addView(btnSave)
        bottomSheetDialog.setContentView(layout); bottomSheetDialog.show()
    }
}