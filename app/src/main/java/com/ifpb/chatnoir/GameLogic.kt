package com.ifpb.chatnoir

import android.util.Log

/**
 * GameLogic.kt
 *
 * Este objeto contém a lógica central do jogo Chat Noir.
 * Ele define:
 * - Movimentos válidos do gato considerando o grid hexagonal (flat-top com odd-offset).
 * - Verificação de vitória do gato e da cerca.
 * - Posicionamento de cercas.
 * - Movimento do gato.
 * - Cálculo de distância do gato até a borda do tabuleiro.
 *
 * É responsável por manipular diretamente o estado do jogo (GameState),
 * validando jogadas e atualizando o tabuleiro.
 */
object GameLogic {
    private val deltasPar = listOf(
        Position(-1, -1), // Noroeste
        Position(-1, 0),  // Nordeste
        Position(0, -1),  // Oeste
        Position(0, 1),   // Leste
        Position(1, -1),  // Sudoeste
        Position(1, 0)    // Sudeste
    )

    private val deltasImpar = listOf(
        Position(-1, 0),  // Noroeste
        Position(-1, 1),  // Nordeste
        Position(0, -1),  // Oeste
        Position(0, 1),   // Leste
        Position(1, 0),   // Sudoeste
        Position(1, 1)    // Sudeste
    )

    fun isValidMove(state: GameState, pos: Position): Boolean {
        return pos.row >= 0 && pos.row <= 10 && pos.col >= 0 && pos.col <= 10 && state.board[pos.row][pos.col] != CellState.FENCE
    }

    // Retorna lista de movimentos possíveis para o gato
    fun getValidCatMoves(state: GameState): List<Position> {
        val currentPos = state.catPosition.value
        val deltas = if (currentPos.row % 2 == 0) deltasPar else deltasImpar
        val validMoves = mutableListOf<Position>()
        for (delta in deltas) {
            val newRow = currentPos.row + delta.row
            val newCol = currentPos.col + delta.col
            val newPos = Position(newRow, newCol)
            if (isValidMove(state, newPos)) {
                validMoves.add(newPos)
            }
        }
        return validMoves
    }

    // Verifica se o gato venceu (chegou na borda)
    fun hasCatWon(state: GameState): Boolean {
        val pos = state.catPosition.value
        return pos.row == 0 || pos.row == 10 || pos.col == 0 || pos.col == 10
    }

    // Verifica se a cerca venceu (gato sem movimentos)
    fun hasFenceWon(state: GameState): Boolean {
        return getValidCatMoves(state).isEmpty()
    }

    // Coloca uma cerca na posição, se for válida
    fun placeFence(state: GameState, pos: Position): Boolean {
        if (pos.row >= 0 && pos.row <= 10 && pos.col >= 0 && pos.col <= 10 && state.board[pos.row][pos.col] == CellState.EMPTY) {
            state.board[pos.row][pos.col] = CellState.FENCE
            return true
        }
        return false
    }

    // Move o gato para a nova posição, se for válida
    fun moveCat(state: GameState, newPos: Position) {
        if (isValidMove(state, newPos)) {
            state.board[state.catPosition.value.row][state.catPosition.value.col] = CellState.EMPTY
            state.board[newPos.row][newPos.col] = CellState.CAT
            state.catPosition.value = newPos
        }
    }

    // Calcula distância do gato até a borda mais próxima
    fun distanceToEdge(pos: Position): Int {
        val top = pos.row
        val bottom = 10 - pos.row
        val left = pos.col
        val right = 10 - pos.col
        return minOf(top, bottom, left, right)
    }
}
