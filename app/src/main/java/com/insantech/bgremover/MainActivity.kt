package com.insantech.bgremover

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.insantech.bgremover.ui.theme.RemovebgTheme
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemovebgTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RemoveBackground()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveBackground() {
    val context = LocalContext.current

    val outputImage: MutableState<Bitmap?> = remember {
        mutableStateOf<Bitmap?>(null)
    }

    val inputImage: MutableState<Bitmap?> = remember {
        mutableStateOf(null)
    }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                inputImage.value =
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        })

    var loading: Boolean by remember {
        mutableStateOf(false)
    }

    var isReal: Boolean by remember {
        mutableStateOf(false)
    }

    val remover = remember {
        RemoveBg(context = context)
    }

    LaunchedEffect(key1 = inputImage.value) {
        inputImage.value?.let { image ->
            remover.clearBackground(image)
                .onStart {
                    loading = true
                }
                .onCompletion {
                    loading = false
                }.collect { output ->
                    outputImage.value = output
                }
        }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.background(Color.White)) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Button to open gallery
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text(text = "Open Gallery")
                    }
                }

                // Usage instructions
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "How to use:")
                Text(text = "1. Take a photo from the gallery.")
                Text(text = "2. Select a photo.")
                Text(text = "3. Let the system work its magic.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Provided by Insantech", style = MaterialTheme.typography.labelSmall)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loading) {
                    CircularProgressIndicator()
                }
                if (outputImage.value != null && inputImage.value != null) {
                    Image(
                        bitmap = if (!isReal) outputImage.value!!.asImageBitmap() else inputImage.value!!.asImageBitmap(),
                        contentDescription = "",
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                isReal = !isReal
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button to save image
                Button(onClick = {
                    val bitmap = outputImage.value!!

                    // Save image to local storage
                    val filename = "output_image_${System.currentTimeMillis()}.png"
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    try {
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            val outputStream = resolver.openOutputStream(it)
                            if (outputStream != null) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            }
                            outputStream?.close()
                            Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(text = "Save to Gallery")
                }
            }
        }
    }
}