package jp.kawagh.bottomapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester


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
        }
        Text("list app")
    }
}