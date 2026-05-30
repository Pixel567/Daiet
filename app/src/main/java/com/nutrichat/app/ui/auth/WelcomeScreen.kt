package com.nutrichat.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nutrichat.app.R

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Updated to use the new logo
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Daiet",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2ECC71) // Matching logo color
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            border = BorderStroke(1.dp, Color(0xFF2ECC71)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Sign up", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2ECC71))
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = { /* TODO */ }) {
            Text(
                "Forgot password?",
                color = Color.Gray,
                textDecoration = TextDecoration.Underline
            )
        }

        TextButton(onClick = { /* TODO */ }) {
            Text(
                "Terms and Privacy Policy",
                color = Color.Gray,
                textDecoration = TextDecoration.Underline
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
