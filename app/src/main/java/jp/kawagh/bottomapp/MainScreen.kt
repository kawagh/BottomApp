package jp.kawagh.bottomapp

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.drawable.toBitmap


@Composable
fun MainScreen() {
    var showTextField by remember {
        mutableStateOf(false)
    }

    var textInput by remember {
        mutableStateOf("")
    }

    val focusRequester = remember {
        FocusRequester()
    }
    var filterOnlyNonSystemApps by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val packageManager = context.packageManager
    val allApps = packageManager.getInstalledApplications(0)
    val systemApps =
        packageManager.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
    val nonSystemApps =
        allApps.filterNot { it.flags.and(ApplicationInfo.FLAG_SYSTEM).compareTo(0) == 1 }
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.White,
                title = {
                    if (showTextField) {
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        TextField(
                            value = textInput, onValueChange = { textInput = it },
                            modifier = Modifier.focusRequester(focusRequester)
                        )
                    } else {
                        Text(stringResource(id = R.string.app_name))
                    }
                },
                actions = {
                    IconButton(onClick = { filterOnlyNonSystemApps = !filterOnlyNonSystemApps }) {
                        val tint =
                            animateColorAsState(
                                targetValue = if (filterOnlyNonSystemApps) Color.Blue else Color.LightGray,
                                animationSpec = tween(durationMillis = 500)
                            )
                        Icon(
                            Icons.Default.FilterList,
                            null,
                            tint = tint.value
                        )
                    }
                }
            )
        },
        content = {
            MainContent(
                showTextField = showTextField,
                textInput = textInput,
                allApps = if (filterOnlyNonSystemApps) nonSystemApps else allApps
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { showTextField = !showTextField }) {
                Icon(Icons.Default.Search, "search app")
            }
        },
    )
}


@Composable
private fun MainContent(
    showTextField: Boolean,
    textInput: String,
    allApps: List<ApplicationInfo>
) {

    val filteredApps = allApps.filter { it.packageName.contains(textInput) }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        if (showTextField) {
            val appsCount =
                if (textInput.isNotEmpty()) {
                    "${filteredApps.size}/${allApps.size} apps"
                } else "${allApps.size} apps"
            Text(
                appsCount, fontSize = MaterialTheme.typography.h5.fontSize,
                modifier = Modifier.align(Alignment.End)
            )
        }
        LazyColumn(horizontalAlignment = Alignment.Start) {
            items(filteredApps) {
                ApplicationInfoItem(
                    appInfo = it,
                    packageManager = LocalContext.current.packageManager
                )
            }
        }
    }
}

@Composable
private fun ApplicationInfoItem(appInfo: ApplicationInfo, packageManager: PackageManager) {
    val context = LocalContext.current
    val appLabel = packageManager.getApplicationLabel(appInfo).toString()
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
                            "launch $appLabel failed",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }) {
        Image(
            bitmap = appInfo.loadIcon(packageManager).toBitmap(150, 150).asImageBitmap(),
            contentDescription = null
        )
        Column() {
            Text(appLabel, fontSize = MaterialTheme.typography.h6.fontSize)
            Text(appInfo.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}