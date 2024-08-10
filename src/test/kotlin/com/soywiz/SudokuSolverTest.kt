package com.soywiz

import kotlin.test.*

class SudokuSolverTest {
    @Test
    fun test() {
        val board = SudokuBoard(intArrayOf(
            0, 0, 7, /**/ 0, 6, 2, /**/ 3, 5, 0,
            0, 5, 1, /**/ 4, 0, 0, /**/ 6, 7, 0,
            3, 0, 0, /**/ 0, 0, 7, /**/ 0, 0, 0,
            ///////////////////////////////////
            1, 0, 0, /**/ 0, 0, 4, /**/ 0, 2, 7,
            2, 0, 9, /**/ 1, 0, 0, /**/ 0, 0, 0,
            0, 0, 5, /**/ 0, 9, 8, /**/ 1, 4, 0,
            ///////////////////////////////////
            0, 3, 0, /**/ 0, 8, 0, /**/ 2, 0, 0,
            9, 0, 0, /**/ 3, 4, 0, /**/ 0, 6, 0,
            7, 6, 0, /**/ 9, 0, 0, /**/ 4, 0, 5,
        ))

        val solver = SudokuSolver(board)
        solver.solveInit()
        for (n in 0 until 10) {
            solver.printBoard()
            val updateCount = solver.updateStep()
            println("updateCount=$updateCount")
            if (updateCount == 0) break
        }

        //println(solver.cells[19].seqs)

        //for (cel in solver.cells) if (cel.missing.count == 1) println(cel)
        //println(solver.cells[0])

        //println(SudokuIndices.rows)
        //println(SudokuIndices.cols)
        //println(SudokuIndices.squares)
    }

    @Test
    fun verifyIndices() {
        assertTrue { SudokuIndices.rows.map { it.indices }.flatten().all { it in 0 .. 80} }
        assertTrue { SudokuIndices.cols.map { it.indices }.flatten().all { it in 0 .. 80} }
        assertTrue { SudokuIndices.squares.map { it.indices }.flatten().all { it in 0 .. 80} }

        assertEquals(81, SudokuIndices.rows.map { it.indices }.flatten().distinct().size)
        assertEquals(81, SudokuIndices.cols.map { it.indices }.flatten().distinct().size)
        assertEquals(81, SudokuIndices.squares.map { it.indices }.flatten().distinct().size)
    }
}