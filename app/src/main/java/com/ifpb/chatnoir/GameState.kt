package com.ifpb.chatnoir

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Position(val row: Int, val col: Int)

enum class CellState {
    EMPTY, CAT, FENCE
}

/**
 * GameState.kt
 *
 * Esta classe representa o estado atual do jogo Chat Noir.
 * Ela mantém:
 * - O tabuleiro (`board`) como uma matriz reativa de células (`CellState`).
 * - A posição atual do gato (`catPosition`).
 * - O placar de vitórias do jogador (`playerWins`) e do gato (`catWins`).
 * - O status atual do jogo (`gameStatus`).
 *
 * No construtor (`init`):
 * - O tabuleiro é inicializado com todas as células vazias (`EMPTY`).
 * - O gato é colocado no centro (posição 5,5).
 * - Um número aleatório de cercas (entre 9 e 15) é colocado aleatoriamente no tabuleiro,
 *   evitando a posição inicial do gato.
 *
 * Esta classe é essencial para que a UI (Jetpack Compose) reaja automaticamente
 * às mudanças no jogo, graças ao uso de `mutableStateOf` e `mutableStateListOf` que são tipos de estados reativos do Jetpack Compose.
 */
class GameState {
    val board = mutableStateListOf<SnapshotStateList<CellState>>()
    val catPosition = mutableStateOf(Position(5, 5))
    val playerWins = mutableStateOf(0)
    val catWins = mutableStateOf(0)
    val gameStatus = mutableStateOf("Vez do jogador")

    init {
        repeat(11) {
            val row = mutableStateListOf<CellState>()
            repeat(11) { row.add(CellState.EMPTY) }
            board.add(row)
        }
        board[5][5] = CellState.CAT
        val fenceCount = (9..15).random()
        repeat(fenceCount) {
            var row: Int
            var col: Int
            do {
                row = (0..10).random()
                col = (0..10).random()
            } while (board[row][col] != CellState.EMPTY || (row == 5 && col == 5))
            board[row][col] = CellState.FENCE
        }
    }
}
