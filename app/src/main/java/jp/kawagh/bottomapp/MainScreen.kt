package jp.kawagh.bottomapp

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.drawable.toBitmap
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalPermissionsApi
@Composable
fun MainScreen() {
    var isGridDisplay by remember {
        mutableStateOf(false)
    }
    var showTextField by remember {
        mutableStateOf(false)
    }

    var textInput by remember {
        mutableStateOf("")
    }

    var filterOnlyNonSystemApps by remember {
        mutableStateOf(false)
    }

    val items = listOf(BottomItem.Home, BottomItem.Recent, BottomItem.Archive)
    var selectedItemIndex by remember {
        mutableStateOf(items.indexOf(BottomItem.Archive))
    }

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
    val onTrailIconClicks: (appInfo: ApplicationInfo) -> Unit = {
        scope.launch {
            when (items[selectedItemIndex]) {
                is BottomItem.Archive -> {
                    dataStore.removeValue(it.packageName)
                }
                else -> {
                    dataStore.saveValue(it.packageName)
                }
            }
        }
    }
    val actionIcon = when (items[selectedItemIndex]) {
        is BottomItem.Archive -> Icons.Default.Remove
        else -> Icons.Default.Archive
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    // TODO launch app on 1 match
    val onDoneClick: () -> Unit = {
        keyboardController?.hide()
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
                            value = textInput,
                            onValueChange = { textInput = it },
                            keyboardActions = KeyboardActions(onDone = { onDoneClick.invoke() }),
                            // when keyboardType is KeyboardType.Ascii ja keyboard appear
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Text(stringResource(id = R.string.app_name))
                    }
                },
                actions = {
                    IconToggleButton(
                        checked = isGridDisplay,
                        onCheckedChange = { isGridDisplay = !isGridDisplay }) {
                        Icon(
                            if (isGridDisplay) Icons.Default.GridOn else Icons.Default.GridOff,
                            null
                        )
                    }
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
                    isGridDisplay = isGridDisplay,
                    showTextField = showTextField,
                    textInput = textInput,
                    appsToDisplay = appsToDisplay,
                    onTrailIconClick = onTrailIconClicks,
                    trailIcon = actionIcon,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(
    isGridDisplay: Boolean,
    showTextField: Boolean,
    textInput: String,
    appsToDisplay: List<ApplicationInfo>,
    onTrailIconClick: (ApplicationInfo) -> Unit,
    trailIcon: ImageVector,
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
        if (isGridDisplay) {
            LazyVerticalGrid(GridCells.Fixed(5)) {
                items(filteredApps) {
                    ApplicationInfoGridItem(applicationInfo = it)
                }
            }
        } else {
            LazyColumn(horizontalAlignment = Alignment.Start) {
                items(filteredApps) {
                    ApplicationInfoItem(
                        appInfo = it,
                        textInput = textInput,
                        onItemClick = { onTrailIconClick(it) },
                        icon = trailIcon,
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationInfoGridItem(applicationInfo: ApplicationInfo) {
    val packageManager = LocalContext.current.packageManager
    val context = LocalContext.current
    val appLabel = packageManager.getApplicationLabel(applicationInfo).toString()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            bitmap = applicationInfo.loadIcon(packageManager).toBitmap(150, 150).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.clickable {
                val intent = packageManager.getLaunchIntentForPackage(applicationInfo.packageName)
                intent?.also {
                    context.startActivity(intent)
                } ?: run {
                    Toast
                        .makeText(
                            context,
                            "launch failed",
                            Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }
        )
        Text(appLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ApplicationInfoItem(
    appInfo: ApplicationInfo,
    textInput: String,
    onItemClick: () -> Unit,
    icon: ImageVector,
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
            Text(
                appLabel,
                fontSize = MaterialTheme.typography.h6.fontSize,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(annotatedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(
            onClick = onItemClick,
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End)
        ) {
            Icon(icon, null)
        }
    }
}