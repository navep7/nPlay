package com.parvatha.music

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.gson.reflect.TypeToken
import com.parvatha.music.databinding.ActivitySettingsBinding
import java.lang.reflect.Type

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var swCrossfade : MaterialSwitch
    private lateinit var swPreviews : MaterialSwitch
    private lateinit var swLockWall: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle("Settings")

        sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        findViewByIds()

        listeners()

        populatePreferences()

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
    }

    private fun listeners() {

        swCrossfade.setOnCheckedChangeListener(
             { buttonView, isChecked ->
                 MainActivity.crossFadeNeeded = isChecked
                 saveSettings("CF", isChecked)
             })

        swPreviews.setOnCheckedChangeListener(
            { buttonView, isChecked ->
                MainActivity.playOnlyPreviews = isChecked
                saveSettings("PR", isChecked)
            })

        swLockWall.setOnCheckedChangeListener(
            { buttonView, isChecked ->
                MainActivity.setLockWall = isChecked
                saveSettings("LW", isChecked)
            })
    }

    private fun saveSettings(pref: String, boolean: Boolean) {

        sharedPreferencesEditor.putBoolean(pref, boolean)
        sharedPreferencesEditor.apply()
        sharedPreferencesEditor.commit()
    }

    fun populatePreferences()  {
        val cf = sharedPreferences.getBoolean("CF", false)
        val pr = sharedPreferences.getBoolean("PR", true)
        val lw = sharedPreferences.getBoolean("LW", true)

        swCrossfade.isChecked = cf
        swPreviews.isChecked = pr
        swLockWall.isChecked = lw
    }

    private fun findViewByIds() {

        swCrossfade = findViewById(R.id.sw_crossfade)
        swPreviews = findViewById(R.id.sw_previews)
        swLockWall = findViewById(R.id.set_lock_wall)
    }

}