package soko.ekibun.fem

import Jama.Matrix

data class Struct(
        val nodes: ArrayList<Node>,
        val elements: ArrayList<Element>,
        val surfaceForces: HashMap<Line, SurfaceForce>,
        val nodeForces: HashMap<Pair<Node, Int>, Double>,
        val surfaceConstraints: HashMap<Line, Node>,
        val nodeConstraints: HashSet<Pair<Node, Int>>
){
    var computed = false
    companion object {
        fun spiltStruct(struct: Struct): Struct{
            val nodesMap = HashMap<Line, Node>()
            val elements = ArrayList<Element>()
            struct.elements.forEach {
                        it.spilt(nodesMap, elements)
                    }
            nodesMap.forEach { _, u ->
                struct.nodes.add(u)
            }
            val surfaceForce = HashMap<Line, SurfaceForce>()
            struct.surfaceForces.forEach { t, u ->
                val ab = nodesMap[Line(t.a, t.b)]!!
                val qab = (u.qa + u.qb) / 2
                surfaceForce[Line(t.a, ab)] = SurfaceForce(u.qa, qab)
                surfaceForce[Line(ab, t.b)] = SurfaceForce(qab, u.qb)
            }
            val surfaceConstraints = HashMap<Line, Node>()
            struct.surfaceConstraints.forEach { t, u ->
                val ab = nodesMap[Line(t.a, t.b)]!!
                surfaceConstraints[Line(t.a, ab)] = u
                surfaceConstraints[Line(ab, t.b)] = u
            }
            return Struct(struct.nodes, elements, surfaceForce, struct.nodeForces, surfaceConstraints, struct.nodeConstraints)
        }
    }
}

fun solve(struct: Struct) {
    //init node dof
    struct.elements.forEach { e ->
        e.nodes.forEach { n ->
            if(n.dof.size < e.node_dof)
                n.dof = IntArray(e.node_dof)
        }
    }
    var dof = 0
    struct.nodes.forEach {
        for(i in it.dof.indices){
            it.dof[i] = dof
            dof++
        }
    }
    val ND = dof
    val K = Matrix(ND, ND)
    struct.elements.forEach {
        var dof_i = 0
        val Ke = it.K
        for(i in it.nodes.indices){
            for(ii in 0 until it.node_dof){
                var dof_j = 0
                for(j in it.nodes.indices){
                    for(ji in 0 until it.node_dof){
                        K[it.nodes[i].dof[ii], it.nodes[j].dof[ji]] += Ke[dof_i, dof_j]
                        dof_j++
                    }
                }
                dof_i++
            }
        }
    }
    K.print(10, 6)
    val F = Matrix(ND, 1)
    struct.surfaceForces.forEach{
        val l = Node.distance(it.key.a, it.key.b)
        println(l)
        val T = Frame.calT(it.key.a, it.key.b)
        val qa = it.value.qa
        val qb = it.value.qb
        val fe = doubleArrayOf(0.0, qa * l * 7 / 20 + qb * l * 3 / 20,
                qa * l * l / 20 + qb * l * l / 30,
                0.0, qb * l * 7 / 20 + qa * l * 3 / 20,
                - qb * l * l / 20 - qa * l * l / 30)
        val Fe = Matrix(arrayOf(fe))
        T.print(10, 6)
        Fe.print(10, 6)

        struct.elements.filter { it is Frame }.map{ it as Frame }.forEach {f ->
            if(Line(f.i, f.j) == it.key)
                f.nodeForces = fe
        }

        val Fd = T * Fe.transpose()
        Fd.print(10, 6)
        F[it.key.a.dof[0], 0] += Fd[0, 0]
        F[it.key.a.dof[1], 0] += Fd[1, 0]
        F[it.key.b.dof[0], 0] += Fd[3, 0]
        F[it.key.b.dof[1], 0] += Fd[4, 0]
        if(it.key.a.dof.size > 2)
            F[it.key.a.dof[2], 0] += Fd[2, 0]
        if(it.key.b.dof.size > 2)
            F[it.key.b.dof[2], 0] += Fd[5, 0]
    }
    struct.nodeForces.forEach{
        F[it.key.first.dof[it.key.second], 0] += it.value
    }
    val bigNum = 1e100
    struct.surfaceConstraints.forEach{
        if(it.value.x > 0){
            K[it.key.a.dof[0], it.key.a.dof[0]] *= bigNum
            //F[it.key.a.dof[0], 0] *= bigNum
            K[it.key.b.dof[0], it.key.b.dof[0]] *= bigNum
            //F[it.key.b.dof[0], 0] *= bigNum
        }
        if(it.value.y > 0){
            K[it.key.a.dof[1], it.key.a.dof[1]] *= bigNum
            //F[it.key.a.dof[1], 0] *= bigNum
            K[it.key.b.dof[1], it.key.b.dof[1]] *= bigNum
            //F[it.key.b.dof[1], 0] *= bigNum
        }
    }
    struct.nodeConstraints.map{ it.first.dof[it.second] }.forEach{
        K[it, it] = K[it, it] * bigNum
        F[it, 0] = F[it, 0] * bigNum
    }
    F.print(15, 10)

    val DISP = K.solve(F)
    DISP.print(15, 10)
    //save data in struct
    val disp = DISP.transpose().array[0]
    struct.nodes.forEach { n ->
        n.disp = DoubleArray(n.dof.size){ disp[n.dof[it]] }
        //n.force = DoubleArray(n.dof.size){ F[n.dof[it], 0] }
    }
    //cal strain
    struct.elements.forEach{
        it.calStrain()
    }
    struct.computed = true
}