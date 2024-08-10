package com.soywiz

class SudokuSolver {
}

object SudokuIndices {
    val rows = List(9) { row(it) }
    val cols = List(9) { col(it) }
    val squares = List(9) { square(it) }

    fun row(n: Int) = List(9) { (n * 9) + it }
    fun col(n: Int) = List(9) { n + (it * 9) }
    fun square(n: Int) = List(9) {
        val offset = ((n % 3) * 3) + ((n / 3) * 27)
        offset + ((it / 3) * 9) + (it % 3)
    }
}
