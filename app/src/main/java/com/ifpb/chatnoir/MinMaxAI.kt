package com.ifpb.chatnoir

import androidx.compose.runtime.mutableStateListOf
import kotlin.math.min

/**
 * MinMaxAI.kt
 *
 * Este objeto implementa a inteligência artificial do gato no jogo Chat Noir,
 * utilizando o algoritmo Minimax para escolher o melhor movimento.
 *
 * Principais componentes:
 * - Avaliação (`evaluate`): atribui pontuações ao estado do jogo.
 *   - Vitória do gato → +10.000
 *   - Vitória do jogador (cercas) → -10.000
 *   - Caso contrário, pontuação baseada na distância do gato até a borda.
 *
 * - Cópia do estado (`copyState`): cria uma cópia profunda do estado do jogo
 *   para simular jogadas sem alterar o estado original.
 *
 * - Possíveis posições de cerca (`getPossibleFencePositions`): retorna todas as
 *   células vazias onde o jogador poderia colocar uma cerca.
 *
 * - Busca do melhor movimento (`findBestMove`): aplica o algoritmo Minimax com
 *   profundidade limitada para escolher o movimento ótimo para o gato.
 *
 * - Algoritmo Minimax (`minMax`): alterna entre fases de maximização (gato)
 *   e minimização (jogador), simulando jogadas futuras até o limite de profundidade.
 *
 * Constantes:
 * - `CAT_WIN_SCORE` = 10000 → vitória do gato
 * - `FENCE_WIN_SCORE` = -10000 → vitória do jogador
 */
object MinMaxAI {
    private const val CAT_WIN_SCORE = 10000
    private const val FENCE_WIN_SCORE = -10000

    /**
     * Avalia a posição do gato no tabuleiro com base na distância até a borda.
     *
     * A função calcula uma pontuação que recompensa o gato por estar mais próximo da borda,
     * pois esse é o objetivo do jogo para o gato.
     *
     * O valor `40` é um fator de escala que multiplica a distância mínima `d` do gato até a borda,
     * convertendo essa distância em uma penalização proporcional na pontuação.
     * Quanto menor `d`, maior será a pontuação (até 500), incentivando o gato a se mover em direção à borda.
     * O resultado é limitado para ficar entre -500 e 500 para evitar valores extremos.
     *
     * Exemplo:
     * - Se o gato estiver na borda (d = 0), a pontuação será próxima de 500 (melhor cenário).
     * - Se estiver mais longe (d maior), a pontuação diminui proporcionalmente.
     */
    private fun evaluate(state: GameState): Int {
        return when {
            GameLogic.hasCatWon(state) -> CAT_WIN_SCORE
            GameLogic.hasFenceWon(state) -> FENCE_WIN_SCORE
            else -> {
                val d = distanceToEdge(state.catPosition.value)
                (500 - d * 40).coerceIn(-500, 500)
            }
        }
    }

    private fun distanceToEdge(pos: Position): Int {
        val top = pos.row
        val bottom = 10 - pos.row
        val left = pos.col
        val right = 10 - pos.col
        return min(min(top, bottom), min(left, right))
    }

    private fun copyState(state: GameState): GameState {
        val newState = GameState()
        newState.board.clear()
        state.board.forEach { row ->
            val newRow = mutableStateListOf<CellState>()
            newRow.addAll(row)
            newState.board.add(newRow)
        }
        newState.catPosition.value = state.catPosition.value.copy()
        return newState
    }

    private fun getPossibleFencePositions(state: GameState): List<Position> {
        val positions = mutableListOf<Position>()
        for (r in 0..10) {
            for (c in 0..10) {
                if (state.board[r][c] == CellState.EMPTY) {
                    positions.add(Position(r, c))
                }
            }
        }
        return positions
    }

    fun findBestMove(state: GameState, depthLimit: Int = 3): Position? {
        var bestScore = Int.MIN_VALUE
        var bestMove: Position? = null
        val moves = GameLogic.getValidCatMoves(state)
        if (moves.isEmpty()) {
            return null
        }
        for (move in moves) {
            val newState = copyState(state)
            GameLogic.moveCat(newState, move)
            val score = minMax(newState, depthLimit - 1, false)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        return bestMove
    }

    private fun minMax(state: GameState, depth: Int, isMaximizing: Boolean): Int {
        if (depth == 0 || GameLogic.hasCatWon(state) || GameLogic.hasFenceWon(state)) {
            return evaluate(state)
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            val moves = GameLogic.getValidCatMoves(state)
            if (moves.isEmpty()) return evaluate(state)
            for (move in moves) {
                val newState = copyState(state)
                GameLogic.moveCat(newState, move)
                val eval = minMax(newState, depth - 1, false)
                maxEval = maxOf(maxEval, eval)
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            val positions = getPossibleFencePositions(state)
            for (pos in positions) {
                val newState = copyState(state)
                GameLogic.placeFence(newState, pos)
                val eval = minMax(newState, depth - 1, true)
                minEval = minOf(minEval, eval)
            }
            return minEval
        }
    }
}
