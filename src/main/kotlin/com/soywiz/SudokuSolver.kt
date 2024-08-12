package com.soywiz

import kotlin.random.*

class SudokuSolver(
    val board: SudokuBoard,
    //val random: Random = Random,
    val random: Random = Random(0),
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

            println("operation[${board.version}]=$operation")
            if (operation != null) ops += operation

            if (isComplete()) {
                printBoard()
                break
            }

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

    fun findEmptyCells() = cells.filter { it.isEmpty }
    fun findSingleCells(): List<SudokuCell> = cells.filter { it.isEmpty && it.missing.count == 1 }
    fun findTwoCandidateCells(): List<SudokuCell> = cells.filter { it.isEmpty && it.missing.count == 2 }
    fun findEmptyCellsSortedByMissingCount(): List<SudokuCell> = findEmptyCells().sortedBy { it.missing.count }

    var step1Count = 0
    var step2Count = 0
    var step3Count = 0

    //val algos = listOf(::updateStep0, ::updateStep1, ::updateStep2)
    //val algos = listOf(::updateStep0, ::updateStep1, ::updateStep2)
    //val algos = listOf(::updateStep0, ::updateStep1, ::updateXYWing)
    //val algos = listOf(::updateStep0, ::updateStep1, ::updateXYWing, ::updateBackTracking)
    val algos = listOf(::updateStep1, ::updateStep0, ::updateXYWing, ::updateBackTracking)
    //val algos = listOf(::updateStep0, ::updateStep1, ::updateBackTracking)
    //val algos = listOf(::updateStep2)

    /**
     * <https://sudokusolver.app/xywing.html>
     */
    fun updateXYWing(): Operation? {
        val wings = arrayListOf<XYWing>()

        for (pivot in findTwoCandidateCells()) {
            val pivotNumbers = pivot.missing.values()
            val pivotRelatedCells = pivot.relatedCells
            val candidates = pivotRelatedCells.filter { it.missing.count == 2 && (it.missing.intersection(pivot.missing).count == 1) }
            //pivot=CELL(50[5, 5], missing[2]=59, included[7]=1234678):
            // - CELL(46[1, 5], missing[2]=59, included[7]=1234678)
            // - CELL(77[5, 8], missing[2]=58, included[7]=1234679)
            // - CELL(39[3, 4], missing[2]=89, included[7]=1234567)
            if (candidates.size >= 2) {
                pincerLoop@for ((index, pincer1) in candidates.withIndex()) {
                    for (pincer2 in candidates.drop(index + 1)) {
                        val intersection = pincer1.missing.intersection(pincer2.missing)
                        if (intersection.count == 1 && intersection.intersection(pivot.missing).count == 0) {
                            //println("  - XY-WING: $pivot, $cand, $cand2")

                            val intersectionOfVisibleToPincer1AndPincer2 = pincer1.relatedCells.toSet().intersect(pincer2.relatedCells.toSet())
                            val stripNum = intersection.findFirstNum()

                            //println("stripNum=$stripNum")
                            var updateCount = 0
                            for (cell in intersectionOfVisibleToPincer1AndPincer2) {
                                //println("-------: $cell - stripNum=$stripNum")
                                val nmissing = cell.missing.without(stripNum)
                                if (cell.missing != nmissing) {
                                    cell.missing = nmissing
                                    updateCount++
                                }
                                //cell.included = cell.missing.inv()
                            }

                            if (updateCount > 0) {
                                wings += XYWing(pivot.index, pincer1.index, pincer2.index, updateCount)
                            }

                            break@pincerLoop
                        }
                    }
                }
            }
        }
        return if (wings.isEmpty()) null else Operation.XY_WING(wings)
    }

    fun updateStep(): Operation? {
        for (algo in algos) {
            if (board.isComplete()) return null
            algo.invoke()?.let { return it }
        }
        for (square in squares) {
            for (cell in square.findEmptyCells()) {
                println(cell)
            }
        }
        TODO()
    }

    var backTrackingLevel = 0

    // Back-tracking
    fun updateBackTracking(): Operation {
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
                println("--- STARTING BACKTRACKING --- $v :: $cellToTry")
                val ops = solve(doInit = false)
                if (isComplete()) {
                    return Operation.BACKTRACKING(ops)
                }
            } catch (e: IllegalBoardException) {
                //println("${Indentations[backTrackingLevel]} -- -- error=${e.message}")
                this.copyFrom(origin)
            } finally {
                backTrackingLevel--
            }
        }
        //println("${Indentations[backTrackingLevel]}/BACKTRACKING: $cellToTry")
        throw IllegalBoardException()
    }

    fun updateStepHiddenSingle(): Operation? {
        //Operation.HIDDEN_SINGLE
        TODO()
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
                it.append(board[n])
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
        get() = board[index]
        set(value) { board[index] = value }

    var value: Int
        get() = _value
        set(value) {
            _value = value
            updateAll()
        }

    val isEmpty get() = value == 0

    val seqs = arrayListOf<SudokuSeqCell>()
    val relatedCells by lazy {
        seqs.flatMap { it.seq.cells }.map { it.cell }.distinct().filter { it != this }
    }
    var missing = SudokuBitMask(0)
    val included get() = missing.inv()

    fun copyFrom(other: SudokuCell) {
        this._value = other._value
        this.missing = other.missing
        for (nseq in seqs.indices) {
            this.seqs[nseq].copyFrom(other.seqs[nseq])
        }
    }

    fun validate() {
        for (seq in seqs) seq.validate()
    }

    fun update() {
        //missing = SudokuBitMask.ALL
        var included = SudokuBitMask.NONE
        for (seq in seqs) {
            //missing = missing.intersection(seq.missing)
            included = included.with(seq.includedPartial)
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
    var missingPartial = SudokuBitMask(0)
    val includedPartial get() = missingPartial.inv()

    var value: Int
        get() = cell.value
        set(value) {
            cell.value = value
        }

    val isEmpty get() = cell.isEmpty

    fun findEmptyCells() = seq.findEmptyCells()

    fun copyFrom(other: SudokuSeqCell) {
        this.missingPartial = other.missingPartial
    }

    fun updated() {
        cell.update()
    }

    fun validate() {
        seq.validate()
    }

    override fun toString(): String = "SudokuSeqCell[${seq.name}](${cell.index}, M=${cell.missing}, I=${cell.included})"
}

class SudokuBoard(val _values: IntArray) {
    constructor(vararg values: Int, unit: Unit = Unit) : this(values)

    var version = 0

    operator fun get(index: Int): Int = _values[index]
    operator fun set(index: Int, value: Int) {
        if (_values[index] == value) return
        _values[index] = value
        version++
    }

    fun isComplete(): Boolean {
        return _values.all { it != 0 }
    }

    init {
        if (_values.size != 81) throw IllegalBoardException()
    }

    fun clone() = SudokuBoard(_values.copyOf())
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

    operator fun get(n: Int) = board[indices[n]]
    fun getMask(): SudokuBitMask {
        var includedMask = SudokuBitMask(0)
        for (n in 0 until 9) {
            val v = this[n]
            if (v == 0) continue
            if (includedMask.has(v)) throw IllegalBoardException()
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
                this.cells[n].missingPartial = missingMask
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
    data class HIDDEN_SINGLE(val count: Int) : Operation()
    data class BACKTRACKING(val ops: List<Operation>) : Operation()
    data class XY_WING(val items: List<XYWing>) : Operation()
}

data class XYWing(val pivot: Int, val pincer1: Int, val pincer2: Int, val stripCount: Int)

inline class SudokuBitMask(val bits: Int) {
    companion object {
        val INVALID = SudokuBitMask(-1)
        val ALL = SudokuBitMask(0b111111111)
        val NONE = SudokuBitMask(0)
    }

    fun mask(value: Int): Int {
        if (value !in 1..9) throw IllegalBoardException()
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

class IllegalBoardException : IllegalStateException()
