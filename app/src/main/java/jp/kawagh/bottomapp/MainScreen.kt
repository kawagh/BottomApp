package jp.kawagh.bottomapp

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.icu.util.Calendar
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import java.util.*

typealias MilliSeconds = Long

@Composable
fun Stats(usageStatsMap: SortedMap<String, MilliSeconds>) {
    LazyColumn(Modifier.size(300.dp)) {
        usageStatsMap.forEach {
            item {
                Text(text = it.key)
                Text(text = Date(it.value).toString())
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
fun MainScreen() {
    var showTextField by remember {
        mutableStateOf(false)
    }

    var textInput by remember {
        mutableStateOf("")
    }

    var filterOnlyNonSystemApps by remember {
        mutableStateOf(false)
    }

    var selectedItemIndex by remember {
        mutableStateOf(0)
    }
    val items = listOf(BottomItem.Home, BottomItem.Recent)

    val context = LocalContext.current
    val packageManager = context.packageManager
    val allApps = packageManager.getInstalledApplications(0)
    val systemApps =
        packageManager.getInstalledApplications(PackageManager.MATCH_SYSTEM_ONLY)
    val nonSystemApps =
        allApps.filterNot { it.flags.and(ApplicationInfo.FLAG_SYSTEM).compareTo(0) == 1 }
    val focusRequester = remember {
        FocusRequester()
    }

    // for calculate Stats
    val usageStatManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val oneMonthAgo = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1)
    }
    val usageStatsMap = usageStatManager.queryUsageStats(
        UsageStatsManager.INTERVAL_MONTHLY,
        oneMonthAgo.timeInMillis,
        System.currentTimeMillis()
    ).associate { it.packageName to it.lastTimeUsed }
    val sortedUsageStatsMap =
        usageStatsMap.toSortedMap { k1, k2 -> (usageStatsMap[k2]!!.compareTo(usageStatsMap[k1]!!)) }
    val sourceApps =
        if (filterOnlyNonSystemApps) nonSystemApps else allApps
    val appsToDisplay = when (items[selectedItemIndex]) {
        BottomItem.Home -> {
            sourceApps
        }
        BottomItem.Recent -> {
            sourceApps.sortedBy {
                -usageStatsMap.getOrDefault(
                    it.packageName,
                    0
                )
            }
        }
    }
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
            Column {
                if (items[selectedItemIndex] == BottomItem.Recent) {
                    Stats(sortedUsageStatsMap)
                }
                MainContent(
                    showTextField = showTextField,
                    textInput = textInput,
                    appsToDisplay = appsToDisplay

                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { showTextField = !showTextField }) {
                Icon(Icons.Default.Search, "search app")
            }
        },
        bottomBar = {
            BottomNavigation(backgroundColor = Color.White) {
                items.forEachIndexed { index, item ->
                    BottomNavigationItem(
                        selected = index == selectedItemIndex,
                        onClick = { selectedItemIndex = index },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.name) },
                    )
                }
            }
        }
    )
}

sealed class BottomItem(val name: String, val icon: ImageVector) {
    object Home : BottomItem("Home", Icons.Default.Home)
    object Recent : BottomItem("Recent", Icons.Default.History)
}

@Composable
private fun MainContent(
    showTextField: Boolean,
    textInput: String,
    appsToDisplay: List<ApplicationInfo>
) {

    val filteredApps = appsToDisplay.filter { it.packageName.contains(textInput) }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val headerText = if (showTextField || textInput.isNotEmpty()) {
            "${filteredApps.size}/${appsToDisplay.size} apps"
        } else "${appsToDisplay.size} apps"
        Text(
            headerText, fontSize = MaterialTheme.typography.h5.fontSize,
            modifier = Modifier.align(Alignment.End)
        )
        LazyColumn(horizontalAlignment = Alignment.Start) {
            items(filteredApps) {
                ApplicationInfoItem(
                    appInfo = it,
                    textInput
                )
            }
        }
    }
}

@Composable
private fun ApplicationInfoItem(appInfo: ApplicationInfo, textInput: String) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val appLabel = packageManager.getApplicationLabel(appInfo).toString()
    val index = appInfo.packageName.indexOf(textInput)
    val annotatedText = buildAnnotatedString {
        if (index == -1) {
            append(appInfo.packageName)
        } else {
            append(appInfo.packageName.substring(0, index))
            withStyle(style = SpanStyle(background = Color.Yellow)) {
                append(appInfo.packageName.substring(index, index + textInput.length))
            }
            append(appInfo.packageName.substring(index + textInput.length))
        }
    }
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
            Text(annotatedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}