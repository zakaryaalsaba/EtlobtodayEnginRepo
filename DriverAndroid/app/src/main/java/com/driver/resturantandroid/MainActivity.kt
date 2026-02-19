package com.driver.resturantandroid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.driver.resturantandroid.databinding.ActivityMainBinding
import com.driver.resturantandroid.ui.auth.LoginActivity
import com.driver.resturantandroid.util.SessionManager
import com.driver.resturantandroid.util.FCMTokenManager
import com.driver.resturantandroid.util.NotificationHelper
import com.driver.resturantandroid.util.LocaleHelper
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Setup notifications (permission + channel)
        NotificationHelper.setupNotifications(this)

        sessionManager = SessionManager(this)
        
        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_orders, R.id.nav_active_delivery, R.id.nav_history, R.id.nav_profile
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Setup drawer header
        setupDrawerHeader()
        
        // Setup online/offline toggle
        setupOnlineStatusToggle()
        
        // Register FCM token if not already registered
        FCMTokenManager.getToken(this)
        
        // Handle notification navigation
        handleNotificationNavigation(intent)
    }
    
    private fun handleNotificationNavigation(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        if (navigateTo == "available_orders") {
            // Navigate to Available Orders tab (it's already the start destination, but ensure it's visible)
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            // If not already on orders tab, navigate to it
            if (navController.currentDestination?.id != R.id.nav_orders) {
                navController.navigate(R.id.nav_orders)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationNavigation(intent)
    }
    
    private fun setupOnlineStatusToggle() {
        val switchOnline = binding.appBarMain.toolbar.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchOnlineStatus)
        
        val driverStatusViewModel = ViewModelProvider(this)[com.driver.resturantandroid.viewmodel.DriverStatusViewModel::class.java]
        val token = sessionManager.getAuthToken()
        
        // Initialize status from session
        val isOnline = sessionManager.isOnline()
        switchOnline.isChecked = isOnline
        updateSwitchColor(switchOnline, isOnline)
        driverStatusViewModel.initializeStatus(isOnline)
        
        switchOnline.setOnCheckedChangeListener { _, isChecked ->
            if (token != null) {
                updateSwitchColor(switchOnline, isChecked)
                driverStatusViewModel.setOnlineStatus(
                    isChecked,
                    token,
                    onSuccess = {
                        sessionManager.setOnlineStatus(isChecked)
                        updateDrawerStatus(isChecked)
                        android.widget.Toast.makeText(
                            this,
                            if (isChecked) getString(R.string.you_are_now_online) else getString(R.string.you_are_now_offline),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { error ->
                        switchOnline.isChecked = !isChecked // Revert
                        updateSwitchColor(switchOnline, !isChecked)
                        val displayMessage = if (isConnectionError(error)) getString(R.string.error_connection) else error
                        android.widget.Toast.makeText(this, displayMessage, android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
    
    private fun updateSwitchColor(switchOnline: com.google.android.material.switchmaterial.SwitchMaterial, isOnline: Boolean) {
        val color = if (isOnline) getColor(R.color.success) else getColor(R.color.error)
        switchOnline.thumbTintList = android.content.res.ColorStateList.valueOf(color)
        switchOnline.trackTintList = android.content.res.ColorStateList.valueOf(color)
    }
    
    private fun isConnectionError(error: String): Boolean {
        val lower = error.lowercase()
        return lower.contains("failed to connect") || lower.contains("unable to resolve") ||
                lower.contains("connection refused") || lower.contains("socket timeout") || lower.contains("connection reset")
    }

    private fun updateDrawerStatus(isOnline: Boolean) {
        val headerView = binding.navView.getHeaderView(0)
        val tvNavStatus = headerView.findViewById<android.widget.TextView>(R.id.tv_nav_status)
        val viewNavStatusIndicator = headerView.findViewById<android.view.View>(R.id.viewNavStatusIndicator)
        
        tvNavStatus?.text = if (isOnline) getString(R.string.online) else getString(R.string.offline)
        viewNavStatusIndicator?.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isOnline) getColor(R.color.success) else getColor(R.color.error)
        )
    }

    private fun setupDrawerHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<android.widget.TextView>(R.id.tv_driver_name)
        val tvEmail = headerView.findViewById<android.widget.TextView>(R.id.tv_driver_email)
        
        tvName?.text = sessionManager.getDriverName() ?: "Driver"
        tvEmail?.text = sessionManager.getDriverEmail() ?: ""
        
        // Update drawer status
        val isOnline = sessionManager.isOnline()
        updateDrawerStatus(isOnline)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language -> {
                showLanguageDialog()
                true
            }
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.english),
            getString(R.string.arabic)
        )
        
        val currentLanguage = LocaleHelper.getLocale(this)
        val currentIndex = if (currentLanguage == "ar") 1 else 0
        
        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguage = if (which == 1) "ar" else "en"
                if (selectedLanguage != currentLanguage) {
                    LocaleHelper.setLocale(this, selectedLanguage)
                    recreate() // Restart activity to apply new locale
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                logout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun logout() {
        sessionManager.clearSession()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}