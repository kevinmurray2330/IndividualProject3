package com.example.individualproject3

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("login") }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var selectedDifficulty by remember { mutableIntStateOf(1) }

    when (currentScreen) {
        "login" -> LoginScreen(
            onLoginSuccess = { user ->
                currentUser = user
                currentScreen = if (user.isParent) "parent_dashboard" else "level_select"
            }
        )
        "parent_dashboard" -> ParentDashboard(
            onLogout = { currentScreen = "login"; currentUser = null }
        )
        "level_select" -> LevelSelectScreen(
            onLevelSelected = { diff ->
                selectedDifficulty = diff
                currentScreen = "game"
            },
            onLogout = { currentScreen = "login"; currentUser = null }
        )
        "game" -> GameScreen(
            user = currentUser!!,
            difficulty = selectedDifficulty,
            onExit = { currentScreen = "level_select" }
        )
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Individual Project 3", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Username") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") })
        Spacer(Modifier.height(16.dp))

        if (isRegistering) {
            Row {
                Button(onClick = {
                    FileHelper.saveUser(context, User(name, pass, false)) // Kid
                    Toast.makeText(context, "Kid Account Created!", Toast.LENGTH_SHORT).show()
                    isRegistering = false
                }) { Text("Register Kid") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    FileHelper.saveUser(context, User(name, pass, true)) // Parent
                    Toast.makeText(context, "Parent Account Created!", Toast.LENGTH_SHORT).show()
                    isRegistering = false
                }) { Text("Register Parent") }
            }
        } else {
            Button(onClick = {
                val users = FileHelper.loadUsers(context)
                val user = users.find { it.name == name && it.pass == pass }
                if (user != null) {
                    onLoginSuccess(user)
                } else {
                    Toast.makeText(context, "Invalid Login", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Login") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { isRegistering = true }) { Text("Create Account") }
        }
    }
}

@Composable
fun ParentDashboard(onLogout: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(FileHelper.loadLogs(context)) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Parent Dashboard", fontSize = 24.sp)
            Button(onClick = onLogout) { Text("Logout") }
        }
        Divider(Modifier.padding(vertical = 8.dp))

        LazyColumn {
            items(logs) { log ->
                Card(Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Child: ${log.childName}", fontWeight = FontWeight.Bold)
                        Text("Level: ${log.level} | Result: ${if(log.success) "Success" else "Fail"}")
                        Text("Time: ${Date(log.timestamp)}")
                    }
                }
            }
        }
    }
}

@Composable
fun LevelSelectScreen(onLevelSelected: (Int) -> Unit, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Select Difficulty", fontSize = 28.sp)
        Spacer(Modifier.height(32.dp))
        Button(modifier = Modifier.size(200.dp, 60.dp), onClick = { onLevelSelected(1) }) {
            Text("Level 1 (Easy)")
        }
        Spacer(Modifier.height(16.dp))
        Button(modifier = Modifier.size(200.dp, 60.dp), onClick = { onLevelSelected(2) }) {
            Text("Level 2 (Hard)")
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("Exit / Logout")
        }
    }
}

@Composable
fun GameScreen(user: User, difficulty: Int, onExit: () -> Unit) {
    val context = LocalContext.current
    val levels = GameContent.levels.filter { it.difficulty == difficulty }
    var currentLevelIndex by remember { mutableIntStateOf(0) }

    val levelData = levels.getOrElse(currentLevelIndex) { levels[0] }
    var characterPos by remember { mutableStateOf(levelData.startRow to levelData.startCol) }
    var commandQueue by remember { mutableStateOf(listOf<String>()) }
    var isPlaying by remember { mutableStateOf(false) }

    fun playWinSound() {
        try {
            val mp = MediaPlayer.create(context, R.raw.success_sound)
            mp?.start()
            mp?.setOnCompletionListener { it.release() }
        } catch (e: Exception) { e.printStackTrace() }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var currentRow = levelData.startRow
            var currentCol = levelData.startCol

            characterPos = currentRow to currentCol
            delay(500)

            var won = false
            for (cmd in commandQueue) {
                when (cmd) {
                    "UP" -> currentRow = (currentRow - 1).coerceAtLeast(0)
                    "DOWN" -> currentRow = (currentRow + 1).coerceAtMost(levelData.gridRows - 1)
                    "LEFT" -> currentCol = (currentCol - 1).coerceAtLeast(0)
                    "RIGHT" -> currentCol = (currentCol + 1).coerceAtMost(levelData.gridCols - 1)
                }

                characterPos = currentRow to currentCol
                delay(600)

                if (levelData.obstacles.contains(currentRow to currentCol)) {
                    Toast.makeText(context, "Hit an obstacle! Try again.", Toast.LENGTH_SHORT).show()
                    isPlaying = false
                    FileHelper.logProgress(context, GameLog(user.name, "Diff $difficulty - Lvl ${currentLevelIndex+1}", false, System.currentTimeMillis()))
                    break
                }
            }

            if (currentRow == levelData.endRow && currentCol == levelData.endCol) {
                won = true
                playWinSound()
                Toast.makeText(context, "Level Complete!", Toast.LENGTH_SHORT).show()
                FileHelper.logProgress(context, GameLog(user.name, "Diff $difficulty - Lvl ${currentLevelIndex+1}", true, System.currentTimeMillis()))
                delay(1000)
                if (currentLevelIndex < levels.size - 1) {
                    currentLevelIndex++
                    commandQueue = emptyList()
                    characterPos = levels[currentLevelIndex].startRow to levels[currentLevelIndex].startCol
                } else {
                    Toast.makeText(context, "All Levels Completed!", Toast.LENGTH_LONG).show()
                    onExit()
                }
            } else if (!won && isPlaying) {
                Toast.makeText(context, "Did not reach goal.", Toast.LENGTH_SHORT).show()
                FileHelper.logProgress(context, GameLog(user.name, "Diff $difficulty - Lvl ${currentLevelIndex+1}", false, System.currentTimeMillis()))
            }
            isPlaying = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Level ${currentLevelIndex + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onExit) { Text("Exit") }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column {
                for (r in 0 until levelData.gridRows) {
                    Row {
                        for (c in 0 until levelData.gridCols) {
                            val isObstacle = levelData.obstacles.contains(r to c)
                            val isEnd = r == levelData.endRow && c == levelData.endCol
                            val isChar = r == characterPos.first && c == characterPos.second

                            Box(
                                Modifier.size(60.dp).border(1.dp, Color.Gray)
                                    .background(
                                        when {
                                            isObstacle -> Color.DarkGray
                                            isEnd -> Color.Green
                                            else -> Color.White
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChar) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = Color.Blue, modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Divider()
        Text("Drag Commands to Queue:", Modifier.padding(8.dp))

        Row(
            Modifier.fillMaxWidth().height(80.dp).background(Color(0xFFEEEEEE)).padding(8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            if (commandQueue.isEmpty()) Text("Queue Empty", Modifier.align(Alignment.CenterVertically))
            commandQueue.forEach { cmd ->
                val (icon, rot) = getIconAndRotation(cmd)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(4.dp).rotate(rot)
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            DraggableCommand("UP") { commandQueue = commandQueue + "UP" }
            DraggableCommand("DOWN") { commandQueue = commandQueue + "DOWN" }
            DraggableCommand("LEFT") { commandQueue = commandQueue + "LEFT" }
            DraggableCommand("RIGHT") { commandQueue = commandQueue + "RIGHT" }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { isPlaying = true }, enabled = !isPlaying) { Text("PLAY") }
            Spacer(Modifier.width(16.dp))
            Button(onClick = { commandQueue = emptyList(); characterPos = levelData.startRow to levelData.startCol }, enabled = !isPlaying) { Text("RESET") }
        }
    }
}

@Composable
fun DraggableCommand(cmd: String, onAdd: () -> Unit) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    val (icon, rotation) = getIconAndRotation(cmd)

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offset.y < -100) {
                            onAdd()
                        }
                        offset = Offset.Zero
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                }
            }
            .size(60.dp)
            .background(Color.Cyan, shape = MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = cmd, modifier = Modifier.rotate(rotation))
    }
}

fun getIconAndRotation(cmd: String): Pair<ImageVector, Float> {
    return when (cmd) {
        "UP" -> Icons.AutoMirrored.Filled.ArrowForward to -90f
        "DOWN" -> Icons.AutoMirrored.Filled.ArrowForward to 90f
        "LEFT" -> Icons.AutoMirrored.Filled.ArrowBack to 0f
        else -> Icons.AutoMirrored.Filled.ArrowForward to 0f
    }
}