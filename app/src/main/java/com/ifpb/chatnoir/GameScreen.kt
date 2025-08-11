package com.ifpb.chatnoir

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .padding(vertical = 20.dp)
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
            if (gameState.board[row][col] == CellState.EMPTY && gameState.gameStatus.value == "Player's turn (Fence)") {
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
                Log.d("GameScreen", "Jogo reiniciado")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Reiniciar Jogo")
        }
    }
}

@Composable
fun Board(gameState: GameState, onCellClick: (Int, Int) -> Unit) {
    Column {
        for (row in 0..10) {
            Row(
                modifier = Modifier.offset(x = if (row % 2 == 1) 15.dp else 0.dp)
            ) {
                for (col in 0..10) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when (gameState.board[row][col]) {
                                    CellState.CAT -> Color.Yellow
                                    CellState.FENCE -> Color.Black
                                    CellState.EMPTY -> Color(0xFF39FF14) // Verde neon
                                }
                            )
                            .clickable { onCellClick(row, col) }
                    )
                }
            }
        }
    }
}