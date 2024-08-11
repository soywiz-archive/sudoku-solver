package com.soywiz

import kotlin.test.*

class SudokuSolverTest {
    // REGLA 1: Mirar filas, columnas y cuadrados para una celda y ver si solo falta un número
    // REGLA 2: ...

    @Test
    fun testLevel2() {
        testSolve(SudokuBoard(
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
    }

    @Test
    fun testLevel3() {
        testSolve(SudokuBoard(
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 6, 3,
            5, 9, 1, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 9, 2, /**/ 0, 0, 5,
            ///////////////////////////////////
            0, 0, 6, /**/ 0, 3, 1, /**/ 2, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            8, 0, 3, /**/ 0, 7, 9, /**/ 5, 0, 0,
            ///////////////////////////////////
            0, 0, 5, /**/ 7, 0, 0, /**/ 0, 0, 9,
            3, 0, 0, /**/ 2, 8, 0, /**/ 6, 7, 0,
            0, 4, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
        ))
    }

    @Test
    fun testLevel3b() {
        val board = SudokuBoard(
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 6, 3,
            5, 9, 1, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 9, 2, /**/ 0, 0, 5,
            /*--------------------------------*/
            0, 0, 6, /**/ 0, 3, 1, /**/ 2, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            8, 2, 3, /**/ 0, 7, 9, /**/ 5, 0, 0,
            /*--------------------------------*/
            0, 0, 5, /**/ 7, 0, 0, /**/ 0, 0, 9,
            3, 1, 9, /**/ 2, 8, 5, /**/ 6, 7, 4,
            0, 4, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
        )
        val solver = SudokuSolver(board)
        solver.solve()
    }

    @Test
    fun testExtremo() {
        testSolve(SudokuBoard(
            0, 0, 2, /**/ 0, 0, 5, /**/ 8, 3, 0,
            7, 0, 0, /**/ 0, 2, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 4, 0,
            ///////////////////////////////////
            0, 0, 1, /**/ 2, 0, 7, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 9, 0, 0,
            0, 8, 0, /**/ 1, 0, 3, /**/ 4, 0, 0,
            ///////////////////////////////////
            0, 9, 0, /**/ 8, 0, 0, /**/ 0, 0, 0,
            6, 0, 0, /**/ 0, 5, 0, /**/ 0, 0, 8,
            0, 3, 0, /**/ 7, 0, 0, /**/ 0, 0, 6,
        ))
    }

    @Test
    fun testEmpty() {
        testSolve(SudokuBoard(
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            ///////////////////////////////////
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            ///////////////////////////////////
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
            0, 0, 0, /**/ 0, 0, 0, /**/ 0, 0, 0,
        ))
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

    private fun testSolve(board: SudokuBoard) {
        val solver = SudokuSolver(board)
        solver.solve()

        //println(solver.cells[19].seqs)

        //for (cel in solver.cells) if (cel.missing.count == 1) println(cel)
        //println(solver.cells[0])

        //println(SudokuIndices.rows)
        //println(SudokuIndices.cols)
        //println(SudokuIndices.squares)
    }
}