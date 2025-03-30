package com.example.booksy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavGraph
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Booksy)

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        setupNavigationGraph()
    }

    private fun setupNavigationGraph() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph) as NavGraph

        // Set start destination based on authentication state
        val user = FirebaseAuth.getInstance().currentUser
        graph.setStartDestination(
            if (user != null) R.id.homeFragment else R.id.loginFragment
        )

        navController.graph = graph
    }

    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val currentDestination = navController.currentDestination?.id

        when (currentDestination) {
            R.id.loginFragment -> {
                // Exit app with double press when in login screen
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    super.onBackPressed()
                } else {
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
            R.id.homeFragment -> {
                // Exit app with double press when in home screen
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    super.onBackPressed()
                } else {
                    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
            R.id.userProfileFragment -> {
                // Go back to home screen from profile
                navController.navigate(R.id.homeFragment)
            }
            else -> {
                // Normal back navigation for other screens
                if (!navController.popBackStack()) {
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Helper methods for global navigation
    fun navigateToHome() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController.navigate(R.id.homeFragment)
    }

    fun navigateToProfile() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController.navigate(R.id.userProfileFragment)
    }
}