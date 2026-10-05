package com.example.kaloriapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class GoalCalculatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_calculator)

        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val rbMale = findViewById<RadioButton>(R.id.rbMale)
        val etAge = findViewById<EditText>(R.id.etCalcAge)
        val etHeight = findViewById<EditText>(R.id.etCalcHeight)
        val etCurrentWeight = findViewById<EditText>(R.id.etCalcCurrentWeight)
        val etTargetWeight = findViewById<EditText>(R.id.etCalcTargetWeight)
        val spinnerActivity = findViewById<Spinner>(R.id.spinnerActivity)
        val btnCreatePlan = findViewById<Button>(R.id.btnCreatePlan)

        // Aktivite Seçenekleri
        val activities = arrayOf("Sedentary (Desk Job)", "Lightly Active (1-3 days/week)", "Moderately Active (3-5 days/week)", "Very Active (6-7 days/week)")
        spinnerActivity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, activities)

        btnCreatePlan.setOnClickListener {
            val isMale = rbMale.isChecked
            val age = etAge.text.toString().toIntOrNull() ?: 0
            val height = etHeight.text.toString().toIntOrNull() ?: 0
            val currentWeight = etCurrentWeight.text.toString().toDoubleOrNull() ?: 0.0
            val targetWeight = etTargetWeight.text.toString().toDoubleOrNull() ?: 0.0

            if (rgGender.checkedRadioButtonId == -1 || age == 0 || height == 0 || currentWeight == 0.0 || targetWeight == 0.0) {
                Toast.makeText(this, "Please fill in all fields correctly.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. BMR Hesaplama (Mifflin-St Jeor Equation)
            val bmr = if (isMale) {
                (10 * currentWeight) + (6.25 * height) - (5 * age) + 5
            } else {
                (10 * currentWeight) + (6.25 * height) - (5 * age) - 161
            }

            // 2. Aktivite Çarpanı
            val multiplier = when (spinnerActivity.selectedItemPosition) {
                0 -> 1.2    // Sedentary
                1 -> 1.375  // Lightly Active
                2 -> 1.55   // Moderately Active
                else -> 1.725 // Very Active
            }

            // 3. Günlük Harcanan Kalori (TDEE)
            var dailyGoal = bmr * multiplier

            // 4. Kilo Alma / Verme Mantığı (Haftada 0.5 kg için ~500 kcal açık/fazla)
            if (targetWeight < currentWeight) {
                dailyGoal -= 500 // Kilo vermek istiyor
            } else if (targetWeight > currentWeight) {
                dailyGoal += 500 // Kilo almak istiyor
            }

            val finalGoal = dailyGoal.toInt()

            // Hafızaya Kaydet ve Ana Sayfaya At
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "Guest"
            val hafiza = getSharedPreferences("KaloriApp_$uid", Context.MODE_PRIVATE)
            hafiza.edit().putInt("HedefKalori", finalGoal).apply()

            Toast.makeText(this, "Plan created! Daily Goal: $finalGoal kcal", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AnaSayfa::class.java))
            finish()
        }
    }
}