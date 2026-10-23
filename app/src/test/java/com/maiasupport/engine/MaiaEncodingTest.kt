package com.maiasupport.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaiaEncodingTest {
    @Test fun startingPositionHasEightRepeatedHistoryPlanes() {
        val tokens = MaiaEncoding.encodeFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(64 * 97, tokens.size)
        assertEquals(1f, tokens[3], 0f) // white rook, a1, plane 0
        assertEquals(1f, tokens[15], 0f) // same square, plane 1
        assertEquals(1f, tokens[87], 0f) // white rook, a1, plane 7
        assertEquals(0f, tokens[64 * 97 - 1], 0f) // clk_ponder channel
    }

    @Test fun blackToMoveMirrorsRanksAndColors() {
        val tokens = MaiaEncoding.encodeFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1")
        assertEquals(1f, tokens[3], 0f) // original black rook becomes oriented white rook on a1
        assertEquals(1f, tokens[56 * 97 + 9], 0f) // white rook mirrored to black rook on a8
    }

    @Test fun moveIndicesMatchMaiaMoveVocabulary() {
        assertEquals(796, MaiaEncoding.moveIndex("e2e4", false))
        assertEquals(796, MaiaEncoding.moveIndex("e7e5", true)) // rank-mirrored for black
        assertEquals(4096, MaiaEncoding.moveIndex("a7a8q", false))
    }

    @Test fun rejectsMalformedFenAndMoves() {
        try {
            MaiaEncoding.encodeFen("not-a-fen")
            throw AssertionError("Expected malformed FEN rejection")
        } catch (_: IllegalArgumentException) { }
        try {
            MaiaEncoding.moveIndex("bad", false)
            throw AssertionError("Expected malformed move rejection")
        } catch (_: IllegalArgumentException) { }
        assertTrue(true)
    }
}
