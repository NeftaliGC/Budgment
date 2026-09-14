package com.nintech.budgment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nintech.budgment.ui.navigation.BudgmentNavHost
import com.nintech.budgment.ui.theme.BudgmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudgmentTheme {
                BudgmentNavHost()
            }
        }
    }
}
