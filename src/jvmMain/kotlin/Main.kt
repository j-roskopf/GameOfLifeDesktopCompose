import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.delay
import java.awt.Dimension
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.*
import kotlin.math.floor

const val CELL_SIZE = 20
const val ROWS = 50
const val COLS = 50
private val initialOffset = 16.dp
private val toolbarHeight = 64.dp
private val fabSize = 64.dp
private val windowPadding = 64.dp
private val toolsPadding = 64.dp
private const val MAX_SPEED = 9f
private const val MIN_SPEED = 1f
private const val STARTING_SPEED = 6f

@Composable
@Preview
fun App(width: Dp, height: Dp) {
    MaterialTheme {
        GameOfLife()
    }
}

fun main() = application {
    Window(
        onCloseRequest = {
            exitApplication()
        },
    ) {
        val insetWidth = window.insets.left.dp + window.insets.right.dp
        val insetHeight = window.insets.top.dp + window.insets.bottom.dp
        window.minimumSize = Dimension(
            (COLS * CELL_SIZE) + insetWidth.value.toInt() + windowPadding.value.toInt() + fabSize.value.toInt(),
            (ROWS * CELL_SIZE) + insetHeight.value.toInt() + windowPadding.value.toInt() + toolbarHeight.value.toInt() + toolsPadding.value.toInt(),
        )
        App(
            width = window.width.dp - window.insets.left.dp - window.insets.right.dp,
            height = window.height.dp - window.insets.top.dp - window.insets.bottom.dp,
        )
    }
}

@Composable
fun GameOfLife() {
    var grid by remember { mutableStateOf(createInitialGrid()) }
    var isPlaying by remember { mutableStateOf(false) }
    var invalidate by remember {
        mutableStateOf(0)
    }
    var generation by remember {
        mutableStateOf(0)
    }
    val sliderValue = remember { mutableStateOf(STARTING_SPEED) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val inverseSpeed = 1001 - (sliderValue.value.toInt() * 100L)
            delay(inverseSpeed)
            grid = getNextGeneration(grid)
            generation++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Conway's Game of Life") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                isPlaying = !isPlaying
            }) {
                if (isPlaying) {
                    Icon(Icons.Default.Clear, contentDescription = "Pause")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                }
            }
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val cellCoordinate = Pair(
                                    floor((offset.x - initialOffset.toPx()) / CELL_SIZE).toInt(),
                                    floor((offset.y - initialOffset.toPx()) / CELL_SIZE).toInt(),
                                )
                                if (cellCoordinate.second < grid.size) {
                                    if (cellCoordinate.first < grid[cellCoordinate.second].size) {
                                        grid[cellCoordinate.second][cellCoordinate.first] = true
                                        invalidate++
                                    }
                                }

                            }
                        )
                    }
            ) {

                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                ) {

                    HorizontalSlider(
                        value = sliderValue,
                        min = MIN_SPEED.toInt(),
                        max = MAX_SPEED.toInt(),
                        onFinished = {
                            sliderValue.value = it.toFloat()
                        }
                    )

                    Text("Speed : ${sliderValue.value}")

                    Text("Generation = $generation")
                }

                Canvas(
                    modifier = Modifier,
                    onDraw = {
                        invalidate.let {
                            // Draw the grid
                            drawGrid()

                            // Draw the cells
                            for (row in 0 until ROWS) {
                                for (col in 0 until COLS) {
                                    if (grid[row][col]) {
                                        val x = col * CELL_SIZE.toFloat()
                                        val y = row * CELL_SIZE.toFloat()
                                        drawRect(
                                            Color.Black,
                                            topLeft = Offset(x + initialOffset.toPx(), y + initialOffset.toPx()),
                                            size = Size(CELL_SIZE.toFloat(), CELL_SIZE.toFloat())
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun HorizontalSlider(value: MutableState<Float>, min: Int, max: Int, onFinished: (Int) -> Unit) {
    Slider(
        modifier = Modifier.width(600.dp),
        value = value.value,
        valueRange = min.toFloat()..max.toFloat(),
        onValueChange = {
            value.value = it.toInt().toFloat()
        },
        onValueChangeFinished = {
            onFinished(value.value.toInt())
        },
    )
}


fun createInitialGrid(): Array<BooleanArray> {
    val grid = Array(ROWS) { BooleanArray(COLS) { false } }
    grid[2][1] = true
    grid[3][2] = true
    grid[3][3] = true
    grid[2][3] = true
    grid[1][3] = true
    return grid
}

fun createEmptyGrid(): Array<BooleanArray> {
    return Array(ROWS) { BooleanArray(COLS) { false } }
}

fun getNextGeneration(grid: Array<BooleanArray>): Array<BooleanArray> {
    val nextGrid = createEmptyGrid()

    for (row in 0 until ROWS) {
        for (col in 0 until COLS) {
            val cell = grid[row][col]
            val numNeighbors = countNeighbors(grid, row, col)

            // Apply the rules of the game
            when {
                cell && (numNeighbors == 2 || numNeighbors == 3) -> nextGrid[row][col] = true // Survival
                cell && numNeighbors > 3 -> nextGrid[row][col] = false // Overpopulation
                cell && numNeighbors < 2 -> nextGrid[row][col] = false // Underpopulation
                !cell && numNeighbors == 3 -> nextGrid[row][col] = true // Reproduction
            }
        }
    }

    return nextGrid
}

fun countNeighbors(grid: Array<BooleanArray>, row: Int, col: Int): Int {
    var count = 0

    for (i in -1..1) {
        for (j in -1..1) {
            val r = row + i
            val c = col + j

            // Make sure we're not counting the cell itself
            if (i == 0 && j == 0) continue

            // Make sure the cell is within the grid
            if (r < 0 || r >= ROWS || c < 0 || c >= COLS) continue

            // Increment the count if the neighbor is alive
            if (grid[r][c]) count++
        }
    }

    return count
}

fun DrawScope.drawGrid() {
    for (i in 0..ROWS) {
        drawLine(
            color = Color.Gray,
            start = Offset(initialOffset.toPx(), i * CELL_SIZE.toFloat() + initialOffset.toPx()),
            end = Offset(COLS * CELL_SIZE.toFloat() + initialOffset.toPx(), (i * CELL_SIZE) + initialOffset.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }

    for (j in 0..COLS) {
        drawLine(
            color = Color.Gray,
            start = Offset(j * CELL_SIZE.toFloat() + initialOffset.toPx(), initialOffset.toPx()),
            end = Offset(
                j * CELL_SIZE.toFloat() + initialOffset.toPx(),
                ROWS * CELL_SIZE.toFloat() + initialOffset.toPx()
            ),
            strokeWidth = 1.dp.toPx()
        )
    }
}