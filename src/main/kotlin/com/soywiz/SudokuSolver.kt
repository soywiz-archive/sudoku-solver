package com.soywiz

class SudokuSolver(val board: SudokuBoard) {
    val cells = List(81) { SudokuCell(this, it) }
    val seqs = SudokuIndices.all.map { SudokuSequence(this, it) }

    fun solveInit() {
        for (seq in seqs) seq.update()
        for (cell in cells) cell.update()
    }

    fun findSingleCells(): List<SudokuCell> = cells.filter { it.missing.count == 1 && it.value == 0 }

    fun updateStep(): Int {
        var count = 0
        for (cell in findSingleCells()) {
            println(cell)
            val v = cell.missing.findFirstNum()
            cell.value = v
            cell.update()
            for (seq in cell.seqs) {
                seq.seq.update()
            }
            count++
        }
        return count
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
        it.appendLine("SudokuBoard(intArrayOf(")
        for (x in 0 until 9) {
            it.append("  ")
            for (y in 0 until 9) {
                it.append("${board.values[n]}, ")
                if (y == 2 || y == 5) it.append("/**/ ")
                n++
            }
            it.appendLine()
            if (x == 2 || x == 5) it.appendLine("  /*--------------------------------*/")
        }
        it.appendLine("))")
    }.toString()
}

class SudokuCell(val solver: SudokuSolver, val index: Int) {
    val board get() = solver.board

    val ncol = index % 9
    val nrow = index / 9
    //val ncol1 = ncol + 1
    //val nrow1 = nrow + 1

    var value: Int
        get() = board.values[index]
        set(value) { board.values[index] = value }

    val seqs = arrayListOf<SudokuSeqCell>()
    var missing = SudokuBitMask(0)
    var included = SudokuBitMask(0)

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

    override fun toString(): String = "CELL($index[$ncol, $nrow], missing[${missing.count}]=$missing, included[${included.count}]=$included)"
}

class SudokuSeqCell(val cell: SudokuCell, val seq: SudokuSequence, val seqIndex: Int) {
    var missing = SudokuBitMask(0)
    var included = SudokuBitMask(0)

    fun updated() {
        cell.update()
    }

    override fun toString(): String = "SudokuSeqCell[${seq.name}](${cell.index}, M=$missing, I=$included)"
}

inline class SudokuBoard(val values: IntArray) {
    init { check(values.size == 81) }
}

class SudokuSequence(val solver: SudokuSolver, val indices: SudokuIndices) {
    val name get() = indices.name
    val board = solver.board
    val cells = List(9) {
        SudokuSeqCell(solver.cells[indices[it]], this, it).also { seqCell ->
            solver.cells[indices[it]].seqs += seqCell
        }
    }

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

    companion object {
        val INVALID = -1
    }
}

class SudokuIndices(val name: String, val indices: List<Int>) {
    operator fun get(n: Int) = indices[n]

    companion object {
        val rows = List(9) { row(it) }
        val cols = List(9) { col(it) }
        val squares = List(9) { square(it) }

        val all = rows + cols + squares

        fun row(n: Int) = SudokuIndices("row$n", List(9) { (n * 9) + it })
        fun col(n: Int) = SudokuIndices("col$n", List(9) { n + (it * 9) })
        fun square(n: Int) = SudokuIndices("square$n", List(9) {
            val offset = ((n % 3) * 3) + ((n / 3) * 27)
            offset + ((it / 3) * 9) + (it % 3)
        })
    }
}

inline class SudokuBitMask(val bits: Int) {
    companion object {
        val INVALID = SudokuBitMask(-1)
        val ALL = SudokuBitMask(0b111111111)
        val NONE = SudokuBitMask(0)
    }

    fun mask(value: Int): Int {
        check(value in 1 .. 9)
        return 1 shl (value - 1)
    }
    fun has(value: Int): Boolean = bits and mask(value) != 0
    fun with(value: Int): SudokuBitMask = SudokuBitMask(bits or mask(value))
    fun without(value: Int): SudokuBitMask = SudokuBitMask(bits and mask(value).inv())
    fun inv(): SudokuBitMask = SudokuBitMask(bits.inv() and 0b111111111)
    fun with(other: SudokuBitMask): SudokuBitMask = SudokuBitMask(this.bits or other.bits)
    fun intersection(other: SudokuBitMask): SudokuBitMask = SudokuBitMask(this.bits and other.bits)
    val count: Int get() = bits.countOneBits()
    fun count(): Int = bits.countOneBits()

    fun findFirstNum(): Int {
        for (n in 1..9) if (has(n)) return n
        return -1
    }

    override fun toString(): String {
        return StringBuilder().also {
            //it.append("${this@SudokuBitMask.count()}:")
            for (n in 1..9) if (this@SudokuBitMask.has(n)) it.append("$n")
        }.toString()
    }
}
