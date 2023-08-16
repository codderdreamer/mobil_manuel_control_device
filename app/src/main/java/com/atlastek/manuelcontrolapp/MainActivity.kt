package com.atlastek.manuelcontrolapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.atlastek.manuelcontrolapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var name : String
    lateinit var password :String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
    }

    private fun initUI() {
        binding.buttonSignIn.setOnClickListener{
            name = binding.editTextName.text.toString()
            password = binding.editTextPassword.text.toString()

            println(name)
            println(password)

            //if(name=="Sevda" && password=="Sevda")
            if(name=="" && password=="")
            {
                val Intent = Intent(this,HandedectActivity::class.java)
                startActivity(Intent)
                binding.falsePassword.visibility = View.INVISIBLE
            }
            else
            {
                binding.falsePassword.visibility = View.VISIBLE
                println("Şifreniz Yanlış!")
            }



        }


    }
}