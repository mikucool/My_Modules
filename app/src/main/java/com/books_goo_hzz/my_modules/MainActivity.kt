package com.books_goo_hzz.my_modules

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.books_goo_hzz.feature_slot.SlotScreen
import com.books_goo_hzz.my_modules.ui.theme.My_ModulesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            My_ModulesTheme {
                SlotScreen()
            }
        }
    }
}
