package com.livetranslator.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livetranslator.app.translation.LanguagePair
import com.livetranslator.app.translation.TranslationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposableMainScreen(
    translationManager: TranslationManager,
    isServiceRunning: Boolean,
    onStartServiceClicked: () -> Unit,
    onStopServiceClicked: () -> Unit,
    onRequestOverlayPermissionClicked: () -> Unit,
    hasOverlayPermission: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPair by remember { mutableStateOf(translationManager.getLanguagePair()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var testInputText by remember { mutableStateOf("Hello, welcome to this video!") }
    var testTranslatedText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Uživo Prevodilac za Video", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isServiceRunning) "Status: Služba je aktivna" else "Status: Služba je zaustavljena",
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text(
                            text = if (isServiceRunning) "Prevodilac hvata zvuk iz TikToka/aplikacija" else "Pritisnite gumb ispod za pokretanje prevođenja",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Language Selector
            Text("Odaberi smjer prevođenja:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPair.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    LanguagePair.entries.forEach { pair ->
                        DropdownMenuItem(
                            text = { Text(pair.displayName) },
                            onClick = {
                                selectedPair = pair
                                translationManager.setLanguagePair(pair)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Overlay Permission Alert if missing
            if (!hasOverlayPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Dozvola za Prikaz Preko Drugih Aplikacija",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Potrebna je dozvola za lebdeći prozor kako bi se prevedeni tekst prikazivao iznad TikToka ili YouTubea.",
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestOverlayPermissionClicked) {
                            Text("Omogući Lebdeći Prozor")
                        }
                    }
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartServiceClicked,
                    enabled = !isServiceRunning && hasOverlayPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pokreni Prevođenje")
                }

                Button(
                    onClick = onStopServiceClicked,
                    enabled = isServiceRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Zaustavi")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Test / Interactive Playground Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧪 Testiraj Prevođenje i Kopiranje", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = testInputText,
                        onValueChange = { testInputText = it },
                        label = { Text("Unesi rečenicu na ${selectedPair.displayName.split("➔")[0].trim()}") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                testTranslatedText = translationManager.translateText(testInputText)
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Prevedi Odmah")
                    }

                    if (testTranslatedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF263238))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Prevedeni Tekst (Njemački / Odabrani jezik):", color = Color(0xFF80CBC4), fontSize = 12.sp)
                                Text(testTranslatedText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Prevedeni Tekst", testTranslatedText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Prevedeni tekst kopiran!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kopiraj Tekst")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
