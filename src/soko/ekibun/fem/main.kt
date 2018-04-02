import soko.ekibun.fem.*
import java.awt.Color
import java.awt.Dimension
import java.awt.geom.Path2D

fun main(vararg args: String){
    val frame = InputFrame()
    frame.size = Dimension(400, 600)
    //frame.isResizable = false
    frame.isVisible = true
    frame.onAction = {
        try{
            val struct = parse(it)
            solve(struct)
            draw(struct)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
/*
    var struct = struct2()
    for(i in 0..3){
        struct = Struct.spiltStruct(struct)
    }
    solve(struct)
    draw(struct.copy())
    */
}

fun draw(struct: Struct){
    var minX = struct.nodes[0].x
    var maxX = struct.nodes[0].x
    var minY = struct.nodes[0].y
    var maxY = struct.nodes[0].y
    struct.nodes.forEach {
        minX = Math.min(minX, it.x)
        maxX = Math.max(maxX, it.x)
        minY = Math.min(minY, it.y)
        maxY = Math.max(maxY, it.y)
        if(struct.computed){
            minX = Math.min(minX, it.x + it.disp[0])
            maxX = Math.max(maxX, it.x + it.disp[0])
            minY = Math.min(minY, it.y + it.disp[1])
            maxY = Math.max(maxY, it.y + it.disp[1])
        }
    }
    val width = maxX-minX
    val height = maxY-minY
    val padding = 100
    println("$width,$height")

    //init maxStrain
    var maxStrain = 0.0
    struct.elements.filter{ it is Triangle}.map{it as Triangle}.forEach {
        maxStrain = Math.max(it.strains.map{Math.abs(it)}.max()!!, maxStrain)
    }
    struct.elements.filter{ it is Triangle}.map{it as Triangle}.forEach {
        it.maxStrain = maxStrain
    }
    println(maxStrain)

    val frame = WinFrame()

    frame.onDraw = ({g ->
        val scale = (frame.width - 2* padding) / width
        //g.scale(scale, scale)
        g.translate(padding - minX * scale, frame.height / 2 + (maxY + minY) * scale / 2)

        //g.color = Color.LIGHT_GRAY
        struct.elements.forEach {
            it.draw(g, scale)
        }
        //g.scale(1/scale, 1/scale)
        g.color = Color.GRAY
        for(i in struct.nodes.indices){
            g.drawString(i.toString(), ((struct.nodes[i].x + struct.nodes[i].disp[0]) * scale).toFloat(), -((struct.nodes[i].y + struct.nodes[i].disp[1]) * scale).toFloat())
        }
    })

    frame.size = Dimension(1000 + 2 * padding, (1000 * height / width).toInt() + 2 * padding)
    frame.isVisible = true
}