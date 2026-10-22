package com.maiasupport.engine

/** Input encoding used by Maia-3's released inference pipeline (history=8, no time features). */
internal object MaiaEncoding {
    private val pieceIndex = mapOf('p' to 0, 'n' to 1, 'b' to 2, 'r' to 3, 'q' to 4, 'k' to 5)
    const val TOKEN_COUNT = 64 * 97

    fun encodeFen(fen: String): FloatArray {
        val fields = fen.trim().split(Regex("\\s+"))
        require(fields.size >= 2) { "FEN must contain board placement and side to move" }
        require(fields[1] == "w" || fields[1] == "b") { "Invalid FEN side to move" }
        val ranks = fields[0].split('/')
        require(ranks.size == 8) { "FEN must contain eight ranks" }
        val blackToMove = fields[1] == "b"
        val board = FloatArray(64 * 12)
        ranks.forEachIndexed { fenRank, rankText ->
            var file = 0
            for (symbol in rankText) {
                if (symbol in '1'..'8') {
                    file += symbol.digitToInt()
                } else {
                    val piece = pieceIndex[symbol.lowercaseChar()]
                        ?: throw IllegalArgumentException("Invalid FEN piece: $symbol")
                    require(file < 8) { "FEN rank is wider than eight squares" }
                    val originalRank = 8 - fenRank
                    val orientedRank = if (blackToMove) 9 - originalRank else originalRank
                    val isWhite = symbol.isUpperCase()
                    val orientedWhite = if (blackToMove) !isWhite else isWhite
                    val square = (orientedRank - 1) * 8 + file
                    val channel = piece + if (orientedWhite) 0 else 6
                    board[square * 12 + channel] = 1f
                    file++
                }
            }
            require(file == 8) { "FEN rank must describe exactly eight squares" }
        }

        // Maia-3 history=8. Without supplied move history, the released UCI
        // default repeats the current board; channel 96 is clk_ponder=0.
        return FloatArray(TOKEN_COUNT).also { input ->
            for (square in 0 until 64) {
                for (history in 0 until 8) {
                    val from = square * 12
                    val to = (square * 97) + history * 12
                    board.copyInto(input, to, from, from + 12)
                }
            }
        }
    }

    fun moveIndex(uci: String, blackToMove: Boolean): Int {
        require(uci.matches(Regex("[a-h][1-8][a-h][1-8][qrbn]?"))) { "Invalid UCI move: $uci" }
        val move = if (blackToMove) mirrorRanks(uci) else uci
        val from = squareIndex(move.substring(0, 2))
        val to = squareIndex(move.substring(2, 4))
        if (move.length == 5) {
            val promotion = when (move[4]) { 'q' -> 0; 'r' -> 1; 'b' -> 2; 'n' -> 3; else -> error("bad promotion") }
            return 4096 + (((move[0] - 'a') * 8 + (move[2] - 'a')) * 4) + promotion
        }
        return from * 64 + to
    }

    private fun squareIndex(square: String) = (square[1] - '1') * 8 + (square[0] - 'a')
    private fun mirrorRanks(uci: String): String = buildString {
        append(uci[0]); append('9' - uci[1]); append(uci[2]); append('9' - uci[3])
        if (uci.length == 5) append(uci[4])
    }
}
