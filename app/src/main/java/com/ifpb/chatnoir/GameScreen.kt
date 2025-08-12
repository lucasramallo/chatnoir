package com.ifpb.chatnoir

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.Canvas

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_scores")

/**
 * GameScreen.kt
 *
 * Componente principal da interface do jogo Chat Noir em Jetpack Compose.
 * Responsável por:
 * - Exibir o tabuleiro.
 * - Mostrar placar e status da partida.
 * - Capturar cliques do jogador e processar jogadas.
 * - Integrar com a IA (MinMaxAI) para movimentar o gato.
 * - Salvar e carregar pontuação usando DataStore.
 *
 * Inclui também o composable Board(), que desenha o grid hexagonal.
 */
@Composable
fun GameScreen() {
    val gameState = remember { GameState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        context.dataStore.data.collect { preferences ->
            gameState.playerWins.value = preferences[intPreferencesKey("player_wins")] ?: 0
            gameState.catWins.value = preferences[intPreferencesKey("cat_wins")] ?: 0
        }
    }

    LaunchedEffect(gameState.board) {
        snapshotFlow { gameState.board.toList() }.collectLatest {
            Log.d("GameScreen", "Tabuleiro atualizado")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .background(Color(0xFFF7FFF1)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Jogador: ${gameState.playerWins.value} Gato: ${gameState.catWins.value}",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = gameState.gameStatus.value,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Board(gameState) { row, col ->
            Log.d("GameScreen", "Clicou em ($row, $col), estado: ${gameState.board[row][col]}, status: ${gameState.gameStatus.value}")
            if (gameState.board[row][col] == CellState.EMPTY && gameState.gameStatus.value == "Vez do jogador") {
                val placed = GameLogic.placeFence(gameState, Position(row, col))
                if (placed) {
                    gameState.gameStatus.value = "Cat's turn"
                    Log.d("GameScreen", "Cerca colocada, turno do gato")
                    if (GameLogic.hasFenceWon(gameState)) {
                        gameState.gameStatus.value = "Cerca venceu!"
                        gameState.playerWins.value += 1
                        CoroutineScope(Dispatchers.IO).launch {
                            context.dataStore.edit { preferences ->
                                preferences[intPreferencesKey("player_wins")] = gameState.playerWins.value
                            }
                        }
                    } else {
                        val bestMove = MinMaxAI.findBestMove(gameState)
                        bestMove?.let {
                            GameLogic.moveCat(gameState, it)
                            gameState.gameStatus.value = "Vez do jogador"
                            Log.d("GameScreen", "Gato moveu para (${it.row}, ${it.col})")
                            if (GameLogic.hasCatWon(gameState)) {
                                gameState.gameStatus.value = "Gato venceu!"
                                gameState.catWins.value += 1
                                CoroutineScope(Dispatchers.IO).launch {
                                    context.dataStore.edit { preferences ->
                                        preferences[intPreferencesKey("cat_wins")] = gameState.catWins.value
                                    }
                                }
                            }
                        } ?: run {
                            Log.d("GameScreen", "Nenhum movimento válido para o gato")
                        }
                    }
                } else {
                    Log.d("GameScreen", "Não foi possível colocar cerca em ($row, $col)")
                }
            } else {
                Log.d("GameScreen", "Clique inválido: célula não vazia ou turno errado")
            }
        }
        Button(
            onClick = {
                gameState.board.clear()
                gameState.catWins.value = 0
                gameState.playerWins.value = 0
                repeat(11) {
                    val row = mutableStateListOf<CellState>()
                    repeat(11) { row.add(CellState.EMPTY) }
                    gameState.board.add(row)
                }
                gameState.board[5][5] = CellState.CAT
                gameState.catPosition.value = Position(5, 5)
                val fenceCount = (9..15).random()
                repeat(fenceCount) {
                    var row: Int
                    var col: Int
                    do {
                        row = (0..10).random()
                        col = (0..10).random()
                    } while (gameState.board[row][col] != CellState.EMPTY || (row == 5 && col == 5))
                    gameState.board[row][col] = CellState.FENCE
                }
                gameState.gameStatus.value = "Vez do jogador"
                Log.d("GameScreen", "Placar zerado")
            },
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF68B207)
            )
        ) {
            Text("Zerar placar")
        }
    }

    if (gameState.gameStatus.value == "Cerca venceu!" || gameState.gameStatus.value == "Gato venceu!") {
        AlertDialog(
            title = { Text("Fim de Jogo") },
            text = { Text(gameState.gameStatus.value) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Reset game when dialog is dismissed
                        gameState.board.clear()
                        repeat(11) {
                            val row = mutableStateListOf<CellState>()
                            repeat(11) { row.add(CellState.EMPTY) }
                            gameState.board.add(row)
                        }
                        gameState.board[5][5] = CellState.CAT
                        gameState.catPosition.value = Position(5, 5)
                        val fenceCount = (9..15).random()
                        repeat(fenceCount) {
                            var row: Int
                            var col: Int
                            do {
                                row = (0..10).random()
                                col = (0..10).random()
                            } while (gameState.board[row][col] != CellState.EMPTY || (row == 5 && col == 5))
                            gameState.board[row][col] = CellState.FENCE
                        }
                        gameState.gameStatus.value = "Vez do jogador"
                        Log.d("GameScreen", "Jogo reiniciado via dialog")
                    }
                ) {
                    Text("Novo Jogo")
                }
            },
            dismissButton = null
        )
    }
}

@Composable
fun HexCell(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(30.dp)
            .clickable { onClick() }
    ) {
        val path = Path()
        val radius = size.minDimension / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (i in 0..5) {
            val angle = Math.toRadians((60.0 * i - 30))
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun Board(gameState: GameState, onCellClick: (Int, Int) -> Unit) {
    Column {
        for (row in 0..10) {
            Row(
                modifier = Modifier
                    .offset(x = if (row % 2 == 1) 18.dp else 0.dp)
            ) {
                for (col in 0..10) {
                    val cellColor = when (gameState.board[row][col]) {
                        CellState.CAT -> Color.Black
                        CellState.FENCE -> Color(0xFF68B207)
                        CellState.EMPTY -> Color(0xFFA0FF1A)
                    }

                    HexCell(
                        color = cellColor,
                        onClick = { onCellClick(row, col) }
                    )
                }
            }
        }
    }
}