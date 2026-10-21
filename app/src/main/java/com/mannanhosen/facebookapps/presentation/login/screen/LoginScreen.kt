package com.mannanhosen.facebookapps.presentation.login.screen
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {

    val fbBlue = Color(0xFF0064E0)
    val inputBorderColor = Color(0xFFCCCCCC)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ){

       Column(
           modifier = Modifier
               .fillMaxSize()
               .padding(horizontal = 24.dp, vertical = 32.dp),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.SpaceBetween
       ) {

           Column(
               modifier = Modifier.fillMaxWidth()
                   .weight(1f),
               horizontalAlignment = Alignment.CenterHorizontally,
               verticalArrangement = Arrangement.Center
           ){

            Text(
                text = "facebook",
                color = fbBlue,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

               OutlinedTextField(

                   value = "",
                   onValueChange = {},
                   singleLine = true,
                   modifier = Modifier
                       .fillMaxWidth()
                       .height(58.dp)
                       .border(
                           width = 1.dp,
                           color = inputBorderColor,
                           shape = RoundedCornerShape(8.dp)
                       ),
                   placeholder = {
                       Text(
                           text = "Email or Phone",
                           color = Color.Gray
                       )
                   },
                   shape = RoundedCornerShape(24.dp),
                   colors = OutlinedTextFieldDefaults.colors(
                       focusedBorderColor = fbBlue,
                       unfocusedBorderColor = inputBorderColor,
                       focusedContainerColor = Color.Transparent,
                       unfocusedContainerColor = Color.Transparent

               ), maxLines = 1,
                   keyboardOptions = KeyboardOptions(
                       keyboardType = KeyboardType.Email,
                       imeAction = ImeAction.Done

                   )


               )

               Spacer(modifier = Modifier.height(16.dp))

               OutlinedTextField(

                   value = "",
                   onValueChange = {},
                   singleLine = true,
                   modifier = Modifier
                       .fillMaxWidth()
                       .height(58.dp)
                       .border(
                           width = 1.dp,
                           color = inputBorderColor,
                           shape = RoundedCornerShape(8.dp)
                       ),
                   placeholder = {
                       Text(
                           text = "Password",
                           color = Color.Gray
                       )
                   },
                   shape = RoundedCornerShape(24.dp),
                   colors = OutlinedTextFieldDefaults.colors(
                       focusedBorderColor = fbBlue,
                       unfocusedBorderColor = inputBorderColor,
                       focusedContainerColor = Color.Transparent,
                       unfocusedContainerColor = Color.Transparent

                   ), maxLines = 1,
                   keyboardOptions = KeyboardOptions(
                       keyboardType = KeyboardType.Email,
                       imeAction = ImeAction.Done

                   )


               )

               Spacer(modifier = Modifier.height(48.dp))


               Button(
                   onClick = { /* Handle Login */ },
                   shape = RoundedCornerShape(24.dp),
                   colors = ButtonDefaults.buttonColors(containerColor = fbBlue),
                   modifier = Modifier
                       .fillMaxWidth()
                       .height(50.dp)
               ) {
                   Text(
                       text = "Log in",
                       fontSize = 16.sp,
                       fontWeight = FontWeight.SemiBold,
                       color = Color.White
                   )
               }

               Spacer(modifier = Modifier.height(12.dp))

               // Forgot password
               TextButton(onClick = { /* Handle Forgot Password */ }) {
                   Text(
                       text = "Forgot password?",
                       color = Color.Black,
                       fontSize = 15.sp,
                       fontWeight = FontWeight.Bold
                   )
               }
           }


           OutlinedButton(
               onClick = { /* Handle Create Account */ },
               shape = RoundedCornerShape(24.dp),
               colors = ButtonDefaults.outlinedButtonColors(
                   containerColor = Color(0xFFF0F2F5)
               ),
               border = ButtonDefaults.outlinedButtonBorder.copy(
                   brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFA2C3E8))
               ),
               modifier = Modifier
                   .fillMaxWidth()
                   .height(48.dp)
           ) {
               Text(
                   text = "Create new account",
                   color = fbBlue,
                   fontSize = 14.sp,
                   fontWeight = FontWeight.Medium
               )
           }
       }




           }
       }






@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
