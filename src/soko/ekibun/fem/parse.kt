package soko.ekibun.fem

import java.util.*

fun parse(s: String): Struct{
    var ANU = 0.0
    var E = 1.0
    var T = 1.0
    var D = Triangle.calD(Element.Type.PLANE_STRESS ,E, ANU)
    var A = 1.0
    var I = 1.0

    var spiltTimes = 0

    val nodes = ArrayList<Node>()
    val elements = ArrayList<Element>()
    val surfaceForces = HashMap<Line, SurfaceForce>()
    val nodeForces = HashMap<Pair<Node, Int>, Double>()
    val surfaceConstraints = HashMap<Line, Node>()
    val nodeConstraints = HashSet<Pair<Node, Int>>()

    s.lines().forEach {
        val scanner = Scanner(it)
        if(!scanner.hasNext())
            return@forEach
        val flag = scanner.next()
        when(flag){
            "SPILT"->{
                spiltTimes = scanner.nextInt()
            }
            "A"->{
                A = scanner.nextDouble()
            }
            "I"->{
                I = scanner.nextDouble()
            }
            "U"->{
                ANU = scanner.nextDouble()
                D = Triangle.calD(Element.Type.PLANE_STRESS ,E, ANU)
            }
            "E"->{
                E = scanner.nextDouble()
                D = Triangle.calD(Element.Type.PLANE_STRESS ,E, ANU)
            }
            "T"->{
                T = scanner.nextDouble()
            }
            "N"->{
                val x = scanner.nextDouble()
                val y = scanner.nextDouble()
                nodes.add(Node(x, y))
            }
            "ET"->{
                val i = scanner.nextInt()
                val j = scanner.nextInt()
                val m = scanner.nextInt()
                elements.add(Triangle(nodes[i], nodes[j], nodes[m], T, D))
            }
            "EF"->{
                val i = scanner.nextInt()
                val j = scanner.nextInt()
                elements.add(Frame(nodes[i], nodes[j], A, E, I))
            }
            "FN"->{
                val n = scanner.nextInt()
                val d = scanner.nextInt()
                val v = scanner.nextDouble()
                nodeForces[Pair(nodes[n], d)] = v
            }
            "FS"->{
                val a = scanner.nextInt()
                val b = scanner.nextInt()
                val va = scanner.nextDouble()
                val vb = scanner.nextDouble()
                surfaceForces[Line(nodes[a], nodes[b])] = SurfaceForce(va, vb)
            }
            "CN"->{
                val n = scanner.nextInt()
                val d = scanner.nextInt()
                nodeConstraints.add(Pair(nodes[n], d))
            }
            "CS"->{
                val a = scanner.nextInt()
                val b = scanner.nextInt()
                val x = scanner.nextDouble()
                val y = scanner.nextDouble()
                surfaceConstraints[Line(nodes[a], nodes[b])] = Node(x, y)
            }
        }
    }
    var struct = Struct(nodes, elements, surfaceForces, nodeForces, surfaceConstraints, nodeConstraints)
    for(i in 0 until spiltTimes)
        struct = Struct.spiltStruct(struct)
    return struct
}

/*
EXP 5.5

E 0.2e9
A 0.763e-2
I 0.1576e-3

N 0 5
N 6.4 5
N 0 0
N 9.6 0

EF 0 1
EF 2 0
EF 1 3

FS 0 1 -60 -60

CN 2 0
CN 2 1
CN 2 2
CN 3 0
CN 3 1
CN 3 2
*/

/*
EXP 2.2

U 0.167
E 2e10
T 1.0

N 0 0
N 0 3
N 9 3
N 9 0

ET 0 2 1
ET 0 3 2

FS 1 2 -10 -10

CS 0 1 1 0
CN 3 1

SPILT 4
*/

/*
EXP 2.1

U 0.0
E 1.0
T 1.0

N 0 2
N 0 0
N 2 0

ET 0 1 2

FN 0 1 -1

CS 0 1 1 0
CS 1 2 0 1

SPILT 4
 */