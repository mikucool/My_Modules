package com.books_goo_hzz.feature_slot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.books_goo_hzz.feature_slot.ui.theme.My_ModulesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SlotActivity : ComponentActivity() {
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
