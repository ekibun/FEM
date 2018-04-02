package soko.ekibun.fem
import Jama.Matrix
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D

data class SurfaceForce(
        val qa: Double,
        val qb: Double){
    fun fa(l: Double): Double {
        return l * (2 * qa + qb) / 6
    }
    fun fb(l: Double): Double {
        return l * (2 * qb + qa) / 6
    }
}

class Node(private vararg val values: Double){
    var dof = intArrayOf()
    //var force = doubleArrayOf()
    var disp = doubleArrayOf()

    val x get() = values[0]
    val y get() = values[1]

    operator fun get(i: Int): Double{
        return values[i]
    }
    companion object {
        fun center(a: Node, b: Node): Node {
            return Node((a.x + b.x) / 2, (a.y + b.y) / 2)
        }
        fun distance(a: Node, b: Node): Double{
            val dx = a.x - b.x
            val dy = a.y - b.y
            return Math.sqrt(dx * dx + dy * dy)
        }
        fun angle(a: Node, b: Node): Double{
            val dx = b.x - a.x
            val dy = b.y - a.y
            return Math.atan2(dy, dx)
        }
    }
}

data class Line(
        val a: Node,
        val b: Node
){
    override fun equals(other: Any?): Boolean {
        return other is Line && ((other.a == a && other.b == b) || (other.b == a && other.a == b))
    }

    override fun hashCode(): Int {
        return a.hashCode() + b.hashCode()
    }
}

abstract class Element{
    abstract val node_dof: Int
    abstract val nodes: Array<Node>
    //cal K
    abstract val K: Matrix
    //cal strains
    abstract fun calStrain()
    //spilt
    open fun spilt(nodesMap: HashMap<Line, Node>, elements: ArrayList<Element>){
        elements.add(this)
    }

    open fun draw(g: Graphics2D, scale: Double){
        val path = Path2D.Double()
        var x = (nodes[0].x + nodes[0].disp[0]) * scale
        var y = -(nodes[0].y + nodes[0].disp[1]) * scale
        path.moveTo(x, y)
        nodes.forEach {
            x = (it.x + it.disp[0]) * scale
            y = -(it.y + it.disp[1]) * scale
            path.lineTo(x, y)
        }
        path.closePath()

        g.color = Color.GRAY
        g.fill(path)
        g.color = Color.GRAY
        g.draw(path)
    }

    enum class Type{
        PLANE_STRESS,
        PLANE_STRAIN
    }
}

//Triangle
class Triangle(val i: Node, val j: Node, val m: Node,
               var T: Double, var D: Matrix): Element(){
    override fun draw(g: Graphics2D, scale: Double) {
        val path = Path2D.Double()
        var x = (nodes[0].x + nodes[0].disp[0]) * scale
        var y = -(nodes[0].y + nodes[0].disp[1]) * scale
        path.moveTo(x, y)
        nodes.forEach {
            x = (it.x + it.disp[0]) * scale
            y = -(it.y + it.disp[1]) * scale
            path.lineTo(x, y)
        }
        path.closePath()

        g.color = Color(Math.abs(strains[0]/maxStrain).toFloat(), Math.abs(strains[1]/maxStrain).toFloat(), Math.abs(strains[2]/maxStrain).toFloat())
        g.fill(path)
        g.color = Color.GRAY
        g.draw(path)
    }

    override fun spilt(nodesMap: HashMap<Line, Node>, elements: ArrayList<Element>) {
        val ij = nodesMap[Line(i, j)]?:Node.center(i, j)
        val jm = nodesMap[Line(j, m)]?:Node.center(j, m)
        val mi = nodesMap[Line(m, i)]?:Node.center(m, i)
        elements.add(Triangle(i, ij, mi, T, D))
        elements.add(Triangle(ij, j, jm, T, D))
        elements.add(Triangle(jm, m, mi, T, D))
        elements.add(Triangle(ij,jm, mi, T, D))

        nodesMap[Line(i, j)] = ij
        nodesMap[Line(j, m)] = jm
        nodesMap[Line(m, i)] = mi
    }

    override val node_dof = 2
    override val nodes = arrayOf(i, j, m)
    var maxStrain = 0.0
    var strains = doubleArrayOf()
    override fun calStrain() {
        val DIS = Matrix(arrayOf(
                doubleArrayOf(i.disp[0], i.disp[1],
                        j.disp[0], j.disp[1],
                        m.disp[0], m.disp[1])))
        strains = (S * DIS.transpose()).transpose().array[0]
    }
    lateinit var S: Matrix
    override val K: Matrix get() {
        S = D * B
        return B.transpose() * S * A * T
    }
    val A: Double by lazy{
        Matrix(arrayOf(
                doubleArrayOf(1.0, i.x, i.y),
                doubleArrayOf(1.0, j.x, j.y),
                doubleArrayOf(1.0, m.x, m.y)
        )).det() / 2
    }
    val B: Matrix by lazy {
        val b = DoubleArray(3)
        val c = DoubleArray(3)
        for(i in b.indices){
            val nodeJ = nodes[(i+1)%3]
            val nodeM = nodes[(i+2)%3]
            b[i] = nodeJ.y - nodeM.y
            c[i] = - nodeJ.x + nodeM.x
        }
        Matrix(arrayOf(
                doubleArrayOf(b[0], 0.0, b[1], 0.0, b[2], 0.0),
                doubleArrayOf(0.0, c[0], 0.0, c[1], 0.0, c[2]),
                doubleArrayOf(c[0],b[0], c[1],b[1], c[2],b[2])
        )) * (1 / ( 2 * A))
    }

    companion object {
        fun calD(NTYPE: Type, E: Double, ANU: Double): Matrix{
            return when(NTYPE){
                Type.PLANE_STRESS -> calPlaneStressD(E, ANU)
                Type.PLANE_STRAIN -> calPlaneStressD(E / (1 - ANU * ANU), ANU / (1 - ANU))
            }
        }
        private fun calPlaneStressD(E: Double, ANU: Double): Matrix{
            return Matrix(arrayOf(
                    doubleArrayOf(1.0, ANU, 0.0),
                    doubleArrayOf(ANU, 1.0, 0.0),
                    doubleArrayOf(0.0, 0.0, (1-ANU)/2)
            )) * (E / (1 - ANU * ANU))
        }
    }
}

//Frame
class Frame(val i: Node, val j: Node, var A: Double, var E: Double, var I: Double): Element() {
    override fun spilt(nodesMap: HashMap<Line, Node>, elements: ArrayList<Element>) {
        val ij = nodesMap[Line(i, j)]?:Node.center(i, j)
        elements.add(Frame(i, ij, A, E, I))
        elements.add(Frame(ij, j, A, E, I))

        nodesMap[Line(i, j)] = ij
    }

    override val node_dof = 3
    override val nodes = arrayOf(i, j)
    val l by lazy { Node.distance(i, j) }
    lateinit var T: Matrix
    lateinit var Ke: Matrix
    override val K: Matrix
        get(){
            T = calT(i, j)
            val EAdl = E * A / l
            val EIdl = E * I / l
            val EIdl2 = EIdl / l
            val EIdl3 = EIdl2 / l
            Ke = Matrix(arrayOf(
                    doubleArrayOf(EAdl,  0.0,        0.0,       -EAdl,   0.0,        0.0),
                    doubleArrayOf(0.0,  12 * EIdl3,  6 * EIdl2,  0.0,  -12 * EIdl3,  6 * EIdl2),
                    doubleArrayOf(0.0,   6 * EIdl2,  4 * EIdl,   0.0,   -6 * EIdl2,  2 * EIdl),
                    doubleArrayOf(-EAdl, 0.0,        0.0,        EAdl,   0.0,        0.0),
                    doubleArrayOf(0.0, -12 * EIdl3, -6 * EIdl2,  0.0,   12 * EIdl3, -6 * EIdl2),
                    doubleArrayOf(0.0,   6 * EIdl2,  2 * EIdl,   0.0,   -6 * EIdl2,  4 * EIdl)))
            Ke.print(10, 6)
            return T * Ke * T.transpose()
        }

    var nodeForces = doubleArrayOf()
    var forces = doubleArrayOf()
    override fun calStrain() {
        val DIS = Matrix(arrayOf(
                doubleArrayOf(i.disp[0], i.disp[1], i.disp[2],
                        j.disp[0], j.disp[1],j.disp[2])))
        DIS.print(10, 6)
        val Fd = Matrix(arrayOf(
                if(nodeForces.size == 6) doubleArrayOf(nodeForces[0], nodeForces[1], nodeForces[2],
                        nodeForces[3], nodeForces[4], nodeForces[5]) else DoubleArray(6)))
        Fd.print(10, 6)
        val Fe = Ke * T.transpose() * DIS.transpose() -  Fd.transpose()
        forces = Fe.transpose().array[0]
        Fe.print(10, 6)
    }

    override fun draw(g: Graphics2D, scale: Double) {
        val path = Path2D.Double()
        val fa = forces[2] / 200
        val fb = -forces[5] / 200
        val ni = Node(i.x + i.disp[0], i.y + i.disp[1])
        val nj = Node(j.x + j.disp[0], j.y + j.disp[1])
        val angle = Node.angle(ni, nj)
        val cosa = Math.cos(angle)
        val sina = Math.sin(angle)
        var x = (ni.x) * scale
        var y = -(ni.y) * scale
        path.moveTo(x, y)
        x = (ni.x - fa * sina) * scale
        y = -(ni.y + fa * cosa) * scale
        path.lineTo(x, y)
        x = (nj.x - fb * sina) * scale
        y = -(nj.y + fb * cosa) * scale
        path.lineTo(x, y)
        x = (nj.x) * scale
        y = -(nj.y) * scale
        path.lineTo(x, y)

        path.closePath()

        g.color = Color.GRAY
        g.draw(path)
    }

    companion object {
        fun calT(i: Node, j: Node): Matrix{
            val angle = Node.angle(i, j)
            println(angle)
            val cosa = Math.cos(angle)
            val sina = Math.sin(angle)
            return Matrix(arrayOf(
                    doubleArrayOf(cosa, -sina, 0.0,  0.0,  0.0,   0.0),
                    doubleArrayOf(sina, cosa,  0.0,  0.0,  0.0,   0.0),
                    doubleArrayOf(0.0,  0.0,   1.0,  0.0,  0.0,   0.0),
                    doubleArrayOf(0.0,  0.0,   0.0,  cosa, -sina, 0.0),
                    doubleArrayOf(0.0,  0.0,   0.0,  sina, cosa,  0.0),
                    doubleArrayOf(0.0,  0.0,   0.0,  0.0,  0.0,   1.0)))
        }
    }
}



