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
import androidx.compose.material.icons.filled.*
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
import androidx.core.graphics.drawable.toBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch


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
    val items = listOf(BottomItem.Home, BottomItem.Recent, BottomItem.Archive)

    val context = LocalContext.current
    val dataStore = ArchiveDataStore(context)
    val archivedPackageNames = dataStore.getValue.collectAsState(initial = emptySet()).value

    LaunchedEffect(Unit) {
        dataStore.saveValue(
            context.packageName
        )
    }
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
    val sourceApps =
        if (filterOnlyNonSystemApps) nonSystemApps else allApps


    val appsToDisplay = when (items[selectedItemIndex]) {
        BottomItem.Home -> {
            sourceApps.filterNot { archivedPackageNames.contains(it.packageName) }
        }
        BottomItem.Recent -> {
            sourceApps
                .filter { usageStatsMap.contains(it.packageName) }
                .filterNot { archivedPackageNames.contains(it.packageName) }
                .sortedBy {
                    -usageStatsMap.getValue(it.packageName)
                }
        }
        BottomItem.Archive -> sourceApps.filter { archivedPackageNames.contains(it.packageName) }
    }
    val scope = rememberCoroutineScope()
    val onItemClick: (appInfo: ApplicationInfo) -> Unit = {
        scope.launch {
            dataStore.saveValue(it.packageName)
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
                MainContent(
                    showTextField = showTextField,
                    textInput = textInput,
                    appsToDisplay = appsToDisplay,
                    onItemClick = onItemClick
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
    object Archive : BottomItem("Archive", Icons.Default.Archive)
}

@Composable
private fun MainContent(
    showTextField: Boolean,
    textInput: String,
    appsToDisplay: List<ApplicationInfo>,
    onItemClick: (ApplicationInfo) -> Unit
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
                    textInput = textInput,
                    onArchiveClick = { onItemClick(it) }
                )
            }
        }
    }
}

@Composable
private fun ApplicationInfoItem(
    appInfo: ApplicationInfo,
    textInput: String,
    onArchiveClick: () -> Unit
) {
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
        Column(

            modifier = Modifier
                .weight(4f)
                .wrapContentWidth(Alignment.Start)
        ) {
            Text(appLabel, fontSize = MaterialTheme.typography.h6.fontSize)
            Text(annotatedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(
            onClick = onArchiveClick,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End)
        ) {
            Icon(Icons.Default.Archive, null)
        }
    }
}