package soko.ekibun.fem

import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame

open class WinFrame : JFrame(){
    var onDraw:(Graphics2D)->Unit = { _->}
    override fun paint(g: Graphics?) {
        super.paint(g)
        g?.let{ onDraw(g as Graphics2D) }
    }
}

open class InputFrame : JFrame(){
    val inputForm = InputForm()
    var onAction:(String)->Unit = { _->}
    init{
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        contentPane = inputForm.panel1
        inputForm.goButton.addActionListener {
            onAction(inputForm.editorPane1.text)
        }
        inputForm.editorPane1.text =
                """
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
"""
    }
    var onDraw:(Graphics2D)->Unit = { _->}
    override fun paint(g: Graphics?) {
        super.paint(g)
        g?.let{ onDraw(g as Graphics2D) }
    }
}