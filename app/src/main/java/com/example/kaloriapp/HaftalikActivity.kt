package com.example.kaloriapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class HaftalikActivity : AppCompatActivity() {

    private lateinit var tvWeekRange: TextView
    private lateinit var tvAvgCal: TextView
    private lateinit var tvTotalWater: TextView
    private lateinit var tvSuccessDays: TextView
    private lateinit var llDaysContainer: LinearLayout
    private lateinit var btnPrevWeek: ImageButton
    private lateinit var btnNextWeek: ImageButton

    private lateinit var hafiza: SharedPreferences
    private var weekOffset = 0 // Haftalık kaydırma
    private var HEDEF_KALORI = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_haftalik)

        tvWeekRange = findViewById(R.id.tvWeekRange)
        tvAvgCal = findViewById(R.id.tvAvgCal)
        tvTotalWater = findViewById(R.id.tvTotalWater)
        tvSuccessDays = findViewById(R.id.tvSuccessDays)
        llDaysContainer = findViewById(R.id.llDaysContainer)
        btnPrevWeek = findViewById(R.id.btnPrevWeek)
        btnNextWeek = findViewById(R.id.btnNextWeek)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "Guest"
        hafiza = getSharedPreferences("KaloriApp_$uid", Context.MODE_PRIVATE)

        loadWeekData()

        btnPrevWeek.setOnClickListener {
            weekOffset--
            loadWeekData()
        }

        btnNextWeek.setOnClickListener {
            if (weekOffset < 0) { // Geleceğe gitmeyi engelle
                weekOffset++
                loadWeekData()
            }
        }

        // NAVBAR KONTROLLERİ
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationViewHaftalik)
        bottomNav.selectedItemId = R.id.nav_haftalik // Haftalık butonunu seçili göster
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ana_sayfa -> {
                    startActivity(Intent(this, AnaSayfa::class.java))
                    true
                }
                R.id.nav_haftalik -> true
                R.id.nav_profil -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sayfa uyanır uyanmaz Haftalık ikonunu seçili yap
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationViewHaftalik)
        bottomNav.selectedItemId = R.id.nav_haftalik
    }

    private fun loadWeekData() {
        HEDEF_KALORI = hafiza.getInt("HedefKalori", 2000)
        llDaysContainer.removeAllViews()

        val cal = Calendar.getInstance(Locale.US)
        cal.firstDayOfWeek = Calendar.MONDAY

        // Seçilen haftaya git
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)

        // Pazartesiye sabitle
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val formatHeader = SimpleDateFormat("MMM d", Locale.US)
        val formatKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatDayName = SimpleDateFormat("EEEE", Locale.US) // Örn: Monday

        val startStr = formatHeader.format(cal.time)

        var totalCals = 0
        var totalWater = 0
        var successCount = 0

        // Pazartesiden Pazara 7 gün döngüsü
        for (i in 0..6) {
            val currentKey = formatKey.format(cal.time)
            val currentDayName = formatDayName.format(cal.time)

            // Hafızadan o günün verilerini çek
            val gunlukKalori = hafiza.getInt("Kalori_$currentKey", 0)
            val gunlukSu = hafiza.getInt("Su_$currentKey", 0)
            val jsonYemekler = hafiza.getString("OgunlerJSON_$currentKey", null)

            totalCals += gunlukKalori
            totalWater += gunlukSu
            if (gunlukKalori in 1..HEDEF_KALORI) successCount++

            // Akordeon Yapısı
            drawDayCard(currentDayName, gunlukKalori, jsonYemekler)

            if (i == 6) { // Pazar günü ise
                val endStr = formatHeader.format(cal.time)
                tvWeekRange.text = "$startStr - $endStr"
            }

            cal.add(Calendar.DAY_OF_MONTH, 1) // Bir sonraki güne geç
        }

        // Özet Kartlarını Güncelle
        tvAvgCal.text = (totalCals / 7).toString()
        tvTotalWater.text = "$totalWater oz"
        tvSuccessDays.text = "$successCount/7"
    }

    private fun drawDayCard(dayName: String, calories: Int, jsonMeals: String?) {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 24)
            }
            setCardBackgroundColor(Color.WHITE)
            radius = 16f
            cardElevation = 2f
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // ÜST KISIM (Her Zaman Görünür)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvDay = TextView(this).apply {
            text = dayName
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#212529"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(0, 24, 1.5f).apply { setMargins(16, 0, 16, 0) }
            max = HEDEF_KALORI
            progress = calories
            // Kalori hedefini aştıysa kırmızı, altındaysa yeşil bar
            progressTintList = android.content.res.ColorStateList.valueOf(
                if (calories > HEDEF_KALORI) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32")
            )
        }

        val tvCal = TextView(this).apply {
            text = if (calories == 0) "No Data" else "$calories kcal"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (calories == 0) Color.GRAY else Color.parseColor("#212529"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        headerRow.addView(tvDay)
        headerRow.addView(progressBar)
        headerRow.addView(tvCal)
        mainContainer.addView(headerRow)

        // ALT KISIM (Yemek Detayları - Başlangıçta Gizli)
        val detailsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 24, 0, 0)
        }

        if (jsonMeals != null && calories > 0) {
            val listType = object : TypeToken<MutableList<OgunModel>>() {}.type
            val mealList: MutableList<OgunModel> = Gson().fromJson(jsonMeals, listType)

            for (meal in mealList) {
                val mealRow = TextView(this).apply {
                    text = "• ${meal.ogunTipi}: ${meal.yemekAdi} (${meal.kalori} kcal)"
                    textSize = 14f
                    setTextColor(Color.parseColor("#6C757D"))
                    setPadding(0, 8, 0, 8)
                }
                detailsContainer.addView(mealRow)
            }
        } else {
            val emptyRow = TextView(this).apply {
                text = "No meals logged this day."
                textSize = 14f
                setTextColor(Color.LTGRAY)
                setPadding(0, 8, 0, 8)
            }
            detailsContainer.addView(emptyRow)
        }

        mainContainer.addView(detailsContainer)
        card.addView(mainContainer)

        // Tıklama ile Akordeon Aç/Kapat (Animasyonlu)
        card.setOnClickListener {
            if (detailsContainer.visibility == View.GONE) {
                detailsContainer.visibility = View.VISIBLE
            } else {
                detailsContainer.visibility = View.GONE
            }
        }

        llDaysContainer.addView(card)
    }
}