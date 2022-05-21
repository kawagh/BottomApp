package jp.kawagh.bottomapp

import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable


@Composable
fun MainScreen() {
    Scaffold(
        content = { Text("main") },
        isFloatingActionButtonDocked = true,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Default.Search, "search app")
            }
        },
        bottomBar = {
            BottomAppBar() {
            }
        }
    )
}
