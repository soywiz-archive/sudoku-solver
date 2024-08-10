package com.soywiz

import kotlin.test.*

class SudokuSolverTest {
    @Test
    fun test() {
        println(SudokuIndices.rows)
        println(SudokuIndices.cols)
        println(SudokuIndices.squares)
    }

    @Test
    fun verifyIndices() {
        assertTrue { SudokuIndices.rows.flatten().all { it in 0 .. 80} }
        assertTrue { SudokuIndices.cols.flatten().all { it in 0 .. 80} }
        assertTrue { SudokuIndices.squares.flatten().all { it in 0 .. 80} }

        assertEquals(81, SudokuIndices.rows.flatten().distinct().size)
        assertEquals(81, SudokuIndices.cols.flatten().distinct().size)
        assertEquals(81, SudokuIndices.squares.flatten().distinct().size)
    }
}