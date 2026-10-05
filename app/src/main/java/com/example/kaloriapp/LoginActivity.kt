package com.example.kaloriapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, AnaSayfa::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmailLogin)
        val etSifre = findViewById<EditText>(R.id.etSifreLogin)
        val btnGiris = findViewById<Button>(R.id.btnGirisYap)
        val btnKayitOlGit = findViewById<Button>(R.id.btnKayitOlGit)

        btnGiris.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val sifre = etSifre.text.toString().trim()

            if (email.isEmpty() || sifre.isEmpty()) {
                Toast.makeText(this, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, sifre)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AnaSayfa::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Login Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnKayitOlGit.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}