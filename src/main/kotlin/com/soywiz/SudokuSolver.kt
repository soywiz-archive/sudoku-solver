package com.soywiz

import kotlin.random.*

class SudokuSolver(
    val board: SudokuBoard,
    val random: Random = Random,
) {
    val cells = List(81) { SudokuCell(this, it) }
    val seqs = SudokuIndices.all.map { SudokuSequence(this, it) }
    val squares = seqs.filter { it.indices.kind == SudokuIndices.Kind.SQUARE }
    val cols = seqs.filter { it.indices.kind == SudokuIndices.Kind.COL }
    val rows = seqs.filter { it.indices.kind == SudokuIndices.Kind.ROW }

    fun isComplete() = board.isComplete()

    operator fun get(index: Int) = cells[index]
    operator fun get(col: Int, row: Int) = cells[row * 9 + col]

    fun copyFrom(other: SudokuSolver) {
        for (ncell in cells.indices) {
            cells[ncell].copyFrom(other.cells[ncell])
        }
    }

    fun clone() = SudokuSolver(board.clone()).also { it.copyFrom(this) }

    fun solve(doInit: Boolean = true): List<Operation> {
        if (doInit) solveInit()
        val ops = arrayListOf<Operation>()
        while (true) {
            printBoard()
            val operation = updateStep()

            println("operation=$operation")
            if (operation != null) ops += operation

            if (isComplete()) {
                printBoard()
                break
            }

            println("operation=$operation")
            if (operation == null) {
                for (cell in findEmptyCellsSortedByMissingCount()) {
                    println("- $cell")
                }
                TODO()
            }
        }
        return ops
    }

    fun solveInit() {
        for (seq in seqs) seq.update()
        for (cell in cells) cell.update()
    }

    fun findSingleCells(): List<SudokuCell> = cells.filter { it.missing.count == 1 && it.value == 0 }
    fun findEmptyCells() = cells.filter { it.isEmpty }
    fun findEmptyCellsSortedByMissingCount(): List<SudokuCell> = findEmptyCells().sortedBy { it.missing.count }

    var step1Count = 0
    var step2Count = 0
    var step3Count = 0

    val algos = listOf(::updateStep0, ::updateStep1, ::updateStep2)

    fun updateStep(): Operation? {
        for (algo in algos) {
            if (board.isComplete()) return null
            algo.invoke()?.let { return it }
        }
        TODO()
    }

    var backTrackingLevel = 0

    // Back-tracking
    fun updateStep2(): Operation {
        val origin = this.clone()
        val cellsToTry = findEmptyCellsSortedByMissingCount()
        val cellsToTryCut = cellsToTry.filter { it.missing.count == cellsToTry.first().missing.count }

        val cellToTry = cellsToTryCut.random(random)
        //println("${Indentations[backTrackingLevel]}BACKTRACKING: $cellToTry")

        for (v in cellToTry.missing.values().sortedBy { random.nextInt() }) {
            //println("${Indentations[backTrackingLevel]} -- try=$v")

            try {
                cellToTry.value = v
                backTrackingLevel++
                Thread.yield()
                val ops = solve(doInit = false)
                if (isComplete()) {
                    return Operation.BACKTRACKING(ops)
                }
            } catch (e: IllegalStateException) {
                //println("${Indentations[backTrackingLevel]} -- -- error=${e.message}")
                this.copyFrom(origin)
            } finally {
                backTrackingLevel--
            }
        }
        //println("${Indentations[backTrackingLevel]}/BACKTRACKING: $cellToTry")
        throw IllegalStateException("No solution here")
    }

    // Same sequences
    fun updateStep0(): Operation? {
        var count = 0
        val singleCells = findSingleCells()
        for (cell in singleCells) {
            //println(cell)
            val v = cell.missing.findFirstNum()
            cell.value = v
            cell.update()
            for (seq in cell.seqs) {
                seq.seq.update()
            }
            count++
        }
        return if (count == 0) null else Operation.SAME_SEQUENCES(count)
    }

    // Complementary sequences
    fun updateStep1(): Operation? {
        var count = 0
        //val sorted = findEmptyCellsSortedByMissingCount()
        //for (cell in sorted) {
        //    println("cell=$cell")
        //}

        for (cell in findEmptyCells()) {
            val cellMissing = cell.missing
            //println("CELL=$cell")
            for (seq in cell.seqs) {
                var set = SudokuBitMask.ALL
                for (cell2 in seq.findEmptyCells()) {
                    if (cell2.cell == cell) continue
                    set = set intersection cell2.cell.included
                    //println("       - $cell2")
                }
                val intersection = cellMissing intersection set
                if (intersection.count == 1) {
                    val value = intersection.findFirstNum()
                    //println("       - SETTING $value")
                    cell.value = value
                    count++
                    //cell.updateAll()
                    break
                }
                //println("!!! cellMissing=$cellMissing -- set=$set -- intersection=${cellMissing intersection set} ::: $seq")
            }
        }
        return if (count == 0) null else Operation.COMPLEMENTARY_SEQUENCES(count)
    }

    fun printBoard() {
        println("$this")
    }

    fun printIndices() {
        var n = 0
        for (x in 0 until 9) {
            for (y in 0 until 9) {
                print("${n.toString().padStart(2, ' ')} ")
                n++
            }
            println()
        }
    }

    override fun toString(): String = StringBuilder(1024).also {
        var n = 0
        it.appendLine("SudokuBoard(")
        for (x in 0 until 9) {
            it.append("  ")
            for (y in 0 until 9) {
                it.append(board.values[n])
                it.append(", ")
                if (y == 2 || y == 5) it.append("/**/ ")
                n++
            }
            it.appendLine()
            if (x == 2 || x == 5) it.appendLine("  /*--------------------------------*/")
        }
        it.appendLine(")")
    }.toString()
}

class SudokuCell(val solver: SudokuSolver, val index: Int) {
    override fun equals(other: Any?): Boolean = other is SudokuCell && other.index == this.index

    val board get() = solver.board

    val ncol = index % 9
    val nrow = index / 9
    //val ncol1 = ncol + 1
    //val nrow1 = nrow + 1

    private var _value: Int
        get() = board.values[index]
        set(value) { board.values[index] = value }

    var value: Int
        get() = _value
        set(value) {
            _value = value
            updateAll()
        }

    val isEmpty get() = value == 0

    val seqs = arrayListOf<SudokuSeqCell>()
    var missing = SudokuBitMask(0)
    var included = SudokuBitMask(0)

    fun copyFrom(other: SudokuCell) {
        this._value = other._value
        this.missing = other.missing
        this.included = other.included
        for (nseq in seqs.indices) {
            this.seqs[nseq].copyFrom(other.seqs[nseq])
        }
    }

    fun validate() {
        for (seq in seqs) seq.validate()
    }

    fun update() {
        //missing = SudokuBitMask.ALL
        included = SudokuBitMask.NONE
        for (seq in seqs) {
            //missing = missing.intersection(seq.missing)
            included = included.with(seq.included)
        }
        missing = included.inv()
        //println("missing[$index]=$missing")
    }

    fun updateAll() {
        for (s in seqs) s.seq.update()
    }

    override fun toString(): String = "CELL($index[$ncol, $nrow], missing[${missing.count}]=$missing, included[${included.count}]=$included)"
}

class SudokuSeqCell(val cell: SudokuCell, val seq: SudokuSequence, val seqIndex: Int) {
    var missing = SudokuBitMask(0)
    var included = SudokuBitMask(0)

    var value: Int
        get() = cell.value
        set(value) {
            cell.value = value
        }

    val isEmpty get() = cell.isEmpty

    fun findEmptyCells() = seq.findEmptyCells()

    fun copyFrom(other: SudokuSeqCell) {
        this.missing = other.missing
        this.included = other.included
    }

    fun updated() {
        cell.update()
    }

    fun validate() {
        seq.validate()
    }

    override fun toString(): String = "SudokuSeqCell[${seq.name}](${cell.index}, M=$missing, I=$included)"
}

inline class SudokuBoard(val values: IntArray) {
    constructor(vararg values: Int, unit: Unit = Unit) : this(values)

    fun isComplete(): Boolean {
        return values.all { it != 0 }
    }

    init {
        check(values.size == 81)
    }

    fun clone() = SudokuBoard(values.copyOf())
}

class SudokuSequence(val solver: SudokuSolver, val indices: SudokuIndices) {
    val name get() = indices.name
    val board = solver.board
    val cells = List(9) {
        SudokuSeqCell(solver.cells[indices[it]], this, it).also { seqCell ->
            solver.cells[indices[it]].seqs += seqCell
        }
    }

    fun findEmptyCells() = cells.filter { it.isEmpty }

    override fun toString(): String = "SudokuSequence($cells)"

    operator fun get(n: Int) = board.values[indices[n]]
    fun getMask(): SudokuBitMask {
        var includedMask = SudokuBitMask(0)
        for (n in 0 until 9) {
            val v = this[n]
            if (v == 0) continue
            check(!includedMask.has(v))
            includedMask = includedMask.with(v)
        }
        //println("includedMask=$includedMask")
        return includedMask
    }

    fun update() {
        val includedMask = getMask()
        val missingMask = includedMask.inv()

        for (n in 0 until 9) {
            val v = this[n]
            if (v == 0) {
                this.cells[n].missing = missingMask
                this.cells[n].included = includedMask
                this.cells[n].updated()
            }
        }

        //println(includedMask.toString())
    }

    fun validate() {
        getMask()
    }

    companion object {
        val INVALID = -1
    }
}

class SudokuIndices(val kind: Kind, val index: Int, val indices: List<Int>) {
    val name: String = "${kind.name.lowercase()}$index"

    enum class Kind { ROW, COL, SQUARE }

    operator fun get(n: Int) = indices[n]

    companion object {
        val rows = List(9) { row(it) }
        val cols = List(9) { col(it) }
        val squares = List(9) { square(it) }

        val all = rows + cols + squares

        fun row(n: Int) = SudokuIndices(Kind.ROW, n, List(9) { (n * 9) + it })
        fun col(n: Int) = SudokuIndices(Kind.COL, n, List(9) { n + (it * 9) })
        fun square(n: Int) = SudokuIndices(Kind.SQUARE, n, List(9) {
            val offset = ((n % 3) * 3) + ((n / 3) * 27)
            offset + ((it / 3) * 9) + (it % 3)
        })
    }
}

sealed class Operation {
    data class SAME_SEQUENCES(val count: Int) : Operation()
    data class COMPLEMENTARY_SEQUENCES(val count: Int) : Operation()
    data class BACKTRACKING(val ops: List<Operation>) : Operation()
}

inline class SudokuBitMask(val bits: Int) {
    companion object {
        val INVALID = SudokuBitMask(-1)
        val ALL = SudokuBitMask(0b111111111)
        val NONE = SudokuBitMask(0)
    }

    fun mask(value: Int): Int {
        check(value in 1..9)
        return 1 shl (value - 1)
    }

    infix fun has(value: Int): Boolean = bits and mask(value) != 0
    infix fun with(value: Int): SudokuBitMask = SudokuBitMask(bits or mask(value))
    infix fun without(value: Int): SudokuBitMask = SudokuBitMask(bits and mask(value).inv())
    fun inv(): SudokuBitMask = SudokuBitMask(bits.inv() and 0b111111111)
    infix fun with(other: SudokuBitMask): SudokuBitMask = SudokuBitMask(this.bits or other.bits)
    infix fun intersection(other: SudokuBitMask): SudokuBitMask = SudokuBitMask(this.bits and other.bits)
    val count: Int get() = bits.countOneBits()
    fun count(): Int = bits.countOneBits()

    fun findFirstNum(): Int {
        for (n in 1..9) if (has(n)) return n
        return -1
    }

    fun values() = (1..9).filter { has(it) }

    override fun toString(): String {
        return StringBuilder().also {
            //it.append("${this@SudokuBitMask.count()}:")
            for (n in 1..9) if (this@SudokuBitMask.has(n)) it.append("$n")
        }.toString()
    }
}

object Indentations {
    val values = arrayListOf<String>("")

    operator fun get(index: Int): String {
        while (index >= values.size) values += values.last() + "  "
        return values[index]
    }
}
