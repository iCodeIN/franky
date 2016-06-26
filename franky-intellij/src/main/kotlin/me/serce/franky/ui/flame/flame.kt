package me.serce.franky.ui.flame

import com.google.protobuf.CodedInputStream
import me.serce.franky.Protocol
import me.serce.franky.Protocol.CallTraceSampleInfo
import java.awt.Dimension
import java.awt.Graphics
import java.io.FileInputStream
import java.util.*
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.SwingUtilities

fun CallTraceSampleInfo.validate() {
    if (frameList.isEmpty()) {
        throw IllegalArgumentException("Empty trace sample $this")
    }
}


class FlameTree(val sampleInfo: List<CallTraceSampleInfo>) {
    val root: FlameNode = FlameNode(0)

    init {
        for (sample in sampleInfo) {
            sample.validate()
            addSampleToTree(sample)
        }
    }

    private fun addSampleToTree(sample: CallTraceSampleInfo) {
        val coef = sample.callCount

        var node = root
        for (frame in sample.frameList) {
            val methodId = frame.jMethodId
            node = node.children.computeIfAbsent(methodId, {
                FlameVertex(coef, FlameNode(frame.jMethodId))
            }).node
        }
        node.selfCost += coef
    }
}

data class FlameVertex(var cost: Int, val node: FlameNode)

class FlameNode(val methodId: Long) {
    var selfCost: Int = 0;
    val children: HashMap<Long, FlameVertex> = hashMapOf()
}

class FlameComponent(val tree: FlameTree) : JComponent() {
    val cellHeigh = 20

    

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        drawLevel(tree.root, g, 0, width, 0)
    }

    private fun drawLevel(node: FlameNode, g: Graphics, begin: Int, end: Int, height: Int) {
        val width = end - begin
        if (width <= 0) {
            return
        }
        g.drawRect(begin, height, width, cellHeigh)
        val totalCost = node.selfCost + node.children.map { it.value.cost }.sum()
        var nodeBegin = begin
        for ((id, vertex) in node.children) {
            val nodeWidth = (width * (vertex.cost / totalCost.toDouble())).toInt()
            drawLevel(vertex.node, g, nodeBegin, nodeBegin + nodeWidth, height + cellHeigh)
            nodeBegin += nodeWidth
        }
    }
}

fun main(args: Array<String>) {
    SwingUtilities.invokeLater {
        val result = Protocol.Response.parseFrom(CodedInputStream.newInstance(FileInputStream("/home/serce/tmp/ResultData")))
        val samples = result.profInfo.samplesList

        val methods = result.profInfo.methodInfosList

        val tree = FlameTree(samples)
        val panel = JFrame().apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            pack()
            size = Dimension(800, 600)
            contentPane.apply {
                add(JLabel("Hello!"))
                add(FlameComponent(tree).apply {
                    size = Dimension(800, 600)
                })
            }
            isVisible = true
        }
    }
}