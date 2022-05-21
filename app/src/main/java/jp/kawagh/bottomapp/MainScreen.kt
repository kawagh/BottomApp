package jp.kawagh.bottomapp

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap


@Composable
fun MainScreen() {
    var showTextField by remember {
        mutableStateOf(false)
    }
    Scaffold(
        content = { MainContent(showTextField = showTextField) },
        isFloatingActionButtonDocked = true,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { showTextField = !showTextField }) {
                Icon(Icons.Default.Search, "search app")
            }
        },
        bottomBar = {
            BottomAppBar() {
            }
        }
    )
}


@Composable
private fun MainContent(showTextField: Boolean) {
    var textInput by remember {
        mutableStateOf("")
    }
    val focusRequester = remember {
        FocusRequester()
    }
    val context = LocalContext.current
    val packageManager = context.packageManager
    val installedAllApps = packageManager.getInstalledApplications(0)
    val filteredApps = installedAllApps.filter { it.packageName.contains(textInput) }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (showTextField) {
            TextField(
                value = textInput, onValueChange = { textInput = it },
                modifier = Modifier.focusRequester(focusRequester)
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
            val appsCount =
                if (textInput.isNotEmpty()) {
                    "${filteredApps.size}/${installedAllApps.size} apps"
                } else "${installedAllApps.size} apps"
            Text(
                appsCount, fontSize = MaterialTheme.typography.h5.fontSize,
                modifier = Modifier.align(Alignment.End)
            )
        }
        LazyColumn(horizontalAlignment = Alignment.Start) {
            items(filteredApps) {
                ApplicationInfoItem(appInfo = it, packageManager = packageManager)
            }
        }
    }
}

@Composable
private fun ApplicationInfoItem(appInfo: ApplicationInfo, packageManager: PackageManager) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                val intent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                intent?.also {
                    context.startActivity(intent)
                } ?: run {
                    Toast
                        .makeText(
                            context,
                            "launch ${appInfo.packageName} failed",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }) {
        Image(
            bitmap = appInfo.loadIcon(packageManager).toBitmap(150, 150).asImageBitmap(),
            contentDescription = null
        )
        Text(appInfo.packageName)
    }
}