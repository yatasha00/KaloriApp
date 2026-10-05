package com.example.kaloriapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kaloriapp.com.example.kaloriapp.FatSecretClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Global Data Model
data class OgunModel(val id: String, val ogunTipi: String, val yemekAdi: String, val kalori: Int)

class AnaSayfa : AppCompatActivity() {

    private lateinit var tvHosgeldin: TextView
    private lateinit var tvTarih: TextView
    private lateinit var tvProfilHarf: TextView
    private lateinit var tvAlinanKalori: TextView
    private lateinit var tvHedefKalori: TextView
    private lateinit var progressBarKalori: ProgressBar
    private lateinit var tvSu: TextView
    private lateinit var tvKarb: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvYag: TextView
    private lateinit var btnOgunEkleCard: com.google.android.material.card.MaterialCardView
    private lateinit var btnSuEkle: Button
    private lateinit var llGununOgunleri: LinearLayout

    private lateinit var hafiza: SharedPreferences
    private lateinit var tarihKey: String

    private var alinanKalori = 0
    private var icilenSuML = 0
    private var karbGram = 0f
    private var proteinGram = 0f
    private var yagGram = 0f
    private var HEDEF_KALORI = 2000

    // Global Categories
    private val ogunTipleri = arrayOf("Breakfast", "Lunch", "Dinner", "Snacks")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ana_sayfa)

        // UI Binding
        tvHosgeldin = findViewById(R.id.tvHosgeldin)
        tvTarih = findViewById(R.id.tvTarih)
        tvProfilHarf = findViewById(R.id.tvProfilHarf)
        tvAlinanKalori = findViewById(R.id.tvAlinanKalori)
        tvHedefKalori = findViewById(R.id.tvHedefKalori)
        progressBarKalori = findViewById(R.id.progressBarKalori)
        tvSu = findViewById(R.id.tvSu)
        tvKarb = findViewById(R.id.tvKarb)
        tvProtein = findViewById(R.id.tvProtein)
        tvYag = findViewById(R.id.tvYag)
        btnOgunEkleCard = findViewById(R.id.btnOgunEkleCard)
        btnSuEkle = findViewById(R.id.btnSuEkle)
        llGununOgunleri = findViewById(R.id.llGununOgunleri)

        // Global Date Format (English Locale)
        val calendar = Calendar.getInstance().time
        tvTarih.text = SimpleDateFormat("MMMM d, EEEE", Locale.US).format(calendar)
        tarihKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "Guest"
        hafiza = getSharedPreferences("KaloriApp_$uid", Context.MODE_PRIVATE)

        if (uid != "Guest") {
            val kayitliIsim = hafiza.getString("KullaniciAdi", "")
            if (!kayitliIsim.isNullOrEmpty()) {
                tvHosgeldin.text = "Hello $kayitliIsim 👋"
                tvProfilHarf.text = kayitliIsim.substring(0, 1).uppercase()
            }

            FirebaseDatabase.getInstance().getReference("Kullanicilar").child(uid).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val realName = snapshot.child("ad").value?.toString() ?: ""
                        if (realName.isNotEmpty() && realName != kayitliIsim) {
                            hafiza.edit().putString("KullaniciAdi", realName).apply()
                            tvHosgeldin.text = "Hello $realName 👋"
                            tvProfilHarf.text = realName.substring(0, 1).uppercase()
                        }
                    }
                }
        }

        updateUI()

        btnSuEkle.setOnClickListener {
            icilenSuML += 8
            hafiza.edit().putInt("Su_$tarihKey", icilenSuML).apply()
            updateUI()
        }

        // YENİ MODERN MENÜYÜ ÇAĞIRAN KISIM
        btnOgunEkleCard.setOnClickListener {
            showAddMealMenu()
        }

        // ALT NAVBAR KONTROLLERİ
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_ana_sayfa -> true
                R.id.nav_haftalik -> {
                    startActivity(Intent(this, HaftalikActivity::class.java))
                    true
                }
                R.id.nav_profil -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun updateUI() {
        HEDEF_KALORI = hafiza.getInt("HedefKalori", 2000)
        alinanKalori = hafiza.getInt("Kalori_$tarihKey", 0)
        icilenSuML = hafiza.getInt("Su_$tarihKey", 0)
        karbGram = hafiza.getFloat("Karb_$tarihKey", 0f)
        proteinGram = hafiza.getFloat("Protein_$tarihKey", 0f)
        yagGram = hafiza.getFloat("Yag_$tarihKey", 0f)

        tvAlinanKalori.text = alinanKalori.toString()
        val remaining = HEDEF_KALORI - alinanKalori
        tvHedefKalori.text = if (remaining >= 0) "Goal: $HEDEF_KALORI kcal - $remaining left"
        else "Goal: $HEDEF_KALORI kcal - ${Math.abs(remaining)} exceeded!"

        progressBarKalori.max = HEDEF_KALORI
        progressBarKalori.progress = alinanKalori
        tvSu.text = "$icilenSuML fl oz"
        tvKarb.text = "${karbGram.toInt()}g"
        tvProtein.text = "${proteinGram.toInt()}g"
        tvYag.text = "${yagGram.toInt()}g"

        drawMealList()
    }

    private fun drawMealList() {
        llGununOgunleri.removeAllViews()
        val list = getMealsFromStorage()

        if (list.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "No meals added yet."
            emptyText.setTextColor(Color.GRAY)
            llGununOgunleri.addView(emptyText)
            return
        }

        for ((index, item) in list.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL

            val tvName = TextView(this).apply {
                text = "${item.ogunTipi} (${item.yemekAdi})"
                setTextColor(Color.parseColor("#212529"))
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvCal = TextView(this).apply {
                text = "${item.kalori} kcal"
                setTextColor(Color.parseColor("#2E7D32"))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.END
            }

            row.addView(tvName)
            row.addView(tvCal)
            llGununOgunleri.addView(row)

            if (index < list.size - 1) {
                val line = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                        setMargins(0, 16, 0, 16)
                    }
                    setBackgroundColor(Color.parseColor("#E9ECEF"))
                }
                llGununOgunleri.addView(line)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_ana_sayfa
    }

    // --- MODERN BOTTOM SHEET TASARIMLARI ---

    private fun showAddMealMenu() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "Select Entry Method"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#212529"))
            setPadding(0, 0, 0, 48)
        }
        layout.addView(title)

        val btnSearch = Button(this).apply {
            text = "🔍 Search Global Database"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150).apply { setMargins(0, 0, 0, 32) }
            setOnClickListener { bottomSheetDialog.dismiss(); showSearchDialog() }
        }
        layout.addView(btnSearch)

        val btnManual = Button(this).apply {
            text = "✍️ Add Manually"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.parseColor("#2E7D32"))
            setBackgroundColor(Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
            setOnClickListener { bottomSheetDialog.dismiss(); showManualEntryDialog() }
        }
        layout.addView(btnManual)

        bottomSheetDialog.setContentView(layout)
        bottomSheetDialog.show()
    }

    private fun showSearchDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply { text = "Search FatSecret"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#212529")); setPadding(0, 0, 0, 32) }

        val inputBg = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#F8F9FA")); cornerRadius = 16f; setStroke(2, Color.parseColor("#E9ECEF")) }

        val input = EditText(this).apply {
            hint = "e.g. Grilled Chicken, Pasta..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(40, 40, 40, 40)
            background = inputBg
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) }
        }

        val btnFind = Button(this).apply {
            text = "FIND FOOD"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
            setOnClickListener {
                if (input.text.isNotEmpty()) { searchFood(input.text.toString()); bottomSheetDialog.dismiss() }
            }
        }

        layout.addView(title); layout.addView(input); layout.addView(btnFind)
        bottomSheetDialog.setContentView(layout)
        bottomSheetDialog.show()
    }

    private fun showManualEntryDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        val title = TextView(this).apply { text = "Manual Entry"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#212529")); setPadding(0, 0, 0, 32) }
        val inputBg = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#F8F9FA")); cornerRadius = 16f; setStroke(2, Color.parseColor("#E9ECEF")) }

        val etName = EditText(this).apply { hint = "Meal Name"; setPadding(40, 40, 40, 40); background = inputBg; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 24) } }
        val etCal = EditText(this).apply { hint = "Calories (kcal)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setPadding(40, 40, 40, 40); background = inputBg; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 32) } }

        val btnNext = Button(this).apply {
            text = "NEXT"
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
            setOnClickListener {
                val name = etName.text.toString()
                val cal = etCal.text.toString().toIntOrNull() ?: 0
                if (name.isNotEmpty() && cal > 0) { bottomSheetDialog.dismiss(); showCategoryDialog(name, cal, 0f, 0f, 0f) }
            }
        }

        layout.addView(title); layout.addView(etName); layout.addView(etCal); layout.addView(btnNext)
        bottomSheetDialog.setContentView(layout); bottomSheetDialog.show()
    }

    private fun showCategoryDialog(name: String, cal: Int, carb: Float, pro: Float, fat: Float) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(64, 64, 64, 64) }
        val title = TextView(this).apply { text = "Select Category"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#212529")); setPadding(0, 0, 0, 32) }
        layout.addView(title)

        val icons = arrayOf("🥞", "🥗", "🥩", "🍎")
        ogunTipleri.forEachIndexed { index, cat ->
            val btn = Button(this).apply {
                text = "${icons[index]}  $cat"
                isAllCaps = false; textSize = 16f; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(48, 0, 0, 0)
                setBackgroundColor(Color.TRANSPARENT); setTextColor(Color.parseColor("#212529"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150)
                setOnClickListener { saveMealData(cat, name, cal, carb, pro, fat); bottomSheetDialog.dismiss() }
            }
            layout.addView(btn)
        }
        bottomSheetDialog.setContentView(layout); bottomSheetDialog.show()
    }

    private fun saveMealData(type: String, name: String, cal: Int, carb: Float, pro: Float, fat: Float) {
        alinanKalori += cal
        karbGram += carb
        proteinGram += pro
        yagGram += fat

        hafiza.edit()
            .putInt("Kalori_$tarihKey", alinanKalori)
            .putFloat("Karb_$tarihKey", karbGram)
            .putFloat("Protein_$tarihKey", proteinGram)
            .putFloat("Yag_$tarihKey", yagGram)
            .apply()

        val list = getMealsFromStorage()
        list.add(OgunModel(System.currentTimeMillis().toString(), type, name, cal))
        hafiza.edit().putString("OgunlerJSON_$tarihKey", Gson().toJson(list)).apply()

        Toast.makeText(this, "Added to $type", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun getMealsFromStorage(): MutableList<OgunModel> {
        val json = hafiza.getString("OgunlerJSON_$tarihKey", null) ?: return mutableListOf()
        return Gson().fromJson(json, object : TypeToken<MutableList<OgunModel>>() {}.type)
    }

    private fun searchFood(query: String) {
        Toast.makeText(this, "Searching database...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response = FatSecretClient.api.searchFood(query)
                val foods = response.asJsonObject.getAsJsonObject("foods")
                if (foods != null && foods.has("food")) {
                    val foodElem = foods.get("food")
                    val results = mutableListOf<com.google.gson.JsonObject>()
                    val names = mutableListOf<String>()

                    if (foodElem.isJsonArray) {
                        for (i in 0 until Math.min(foodElem.asJsonArray.size(), 5)) {
                            val obj = foodElem.asJsonArray[i].asJsonObject
                            results.add(obj); names.add(obj.get("food_name").asString)
                        }
                    } else {
                        val obj = foodElem.asJsonObject
                        results.add(obj); names.add(obj.get("food_name").asString)
                    }

                    // Arama sonuçları için şimdilik orijinal dialog kalsın (Listeleme kolaylığı için)
                    AlertDialog.Builder(this@AnaSayfa)
                        .setTitle("Search Results")
                        .setItems(names.toTypedArray()) { _, index ->
                            val selected = results[index]
                            val foodName = selected.get("food_name").asString
                            val desc = selected.get("food_description").asString

                            val cal = Regex("Calories:\\s*(\\d+)").find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                            val carb = Regex("Carbs:\\s*([\\d.]+)").find(desc)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                            val pro = Regex("Protein:\\s*([\\d.]+)").find(desc)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                            val fat = Regex("Fat:\\s*([\\d.]+)").find(desc)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

                            AlertDialog.Builder(this@AnaSayfa)
                                .setTitle(foodName)
                                .setMessage("$desc\n\nAdd to your log?")
                                .setPositiveButton("Add") { _, _ -> showCategoryDialog(foodName, cal, carb, pro, fat) }
                                .setNegativeButton("Back", null).show()
                        }.show()
                } else {
                    Toast.makeText(this@AnaSayfa, "No results found.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AnaSayfa, "Connection failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}