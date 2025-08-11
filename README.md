# Chat Noir

Projeto desenvolvido em Kotlin usando Jetpack Compose que implementa o jogo Chat Noir. O jogo conta com uma inteligência artificial baseada no algoritmo Minimax para controlar o movimento do gato, enquanto o jogador posiciona cercas para tentar capturá-lo.

## Descrição

Este projeto apresenta uma interface gráfica responsiva feita com Jetpack Compose, um tabuleiro para o jogo, e uma IA para o gato utilizando o algoritmo Minimax. O estado do jogo é gerenciado de forma reativa, garantindo atualizações automáticas da interface. Além disso, o placar de vitórias é salvo localmente usando DataStore.

## Tecnologias e bibliotecas utilizadas

- Kotlin
- Jetpack Compose (UI declarativa)
- Coroutines (para operações assíncronas)
- DataStore Preferences (armazenamento local)
- Algoritmo Minimax (para IA)
- Android Studio (IDE)

## Estrutura principal

- `GameScreen.kt`: Composable principal que controla o fluxo do jogo e a UI.
- `GameState.kt`: Classe que mantém o estado atual do jogo.
- `MinMaxAI.kt`: Implementação da inteligência artificial para o gato.
- `GameLogic.kt`: Regras do jogo e manipulação do estado do tabuleiro.

## Como executar

1. Abra o projeto no Android Studio.
2. Compile e execute em um emulador ou dispositivo Android.
3. Interaja com o tabuleiro para jogar contra a IA.

## Gameplay

https://github.com/user-attachments/assets/2f7ee9b9-8d95-4446-8a27-b9df9222bdcf

