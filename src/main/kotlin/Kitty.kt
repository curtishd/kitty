package me.cdh

import me.cdh.Action.*
import java.awt.Graphics
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.timer

object Kitty : JPanel() {

    init {
        SwingUtilities.invokeLater { initSystemTray() }
        isOpaque = false
        window.isVisible = true
        changeAction(CURLED)
        timer(initialDelay = 10L, period = 10L, action = {
            updateAction()
            doAction()
            updateAnimation()
            bubbleState()
            window.repaint()
        })
        if (isDayTime()) timer(initialDelay = 30000L, period = 30000L, action = { tryWander() })
        else timer(initialDelay = 6000L, period = 6000L, action = { tryWander() })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        var cImg = currFrames?.get(frameNum)
        if ((action == LAYING || action == RISING || action == SLEEP) && layingDir == Direction.LEFT || action == CURLED && layingDir == Direction.RIGHT)
            cImg = cImg?.let { flipImage(it) }
        g?.drawImage(cImg, 0, 0, 100, 100, null)
        if (bubbleState != BubbleState.NONE) {
            val currImg = currBubbleFrames?.get(bubbleFrameNum)
            var x = 30
            var y = 40
            when (action) {
                SLEEP, LAYING, LEFT, RIGHT -> {
                    if (layingDir == Direction.LEFT) x -= 30 else x += 30
                }

                UP, LICKING, SITTING -> y -= 25
                else -> {}
            }
            g?.drawImage(currImg, x, y, 30, 30, null)
        }
    }

    private fun readResolve(): Any = Kitty
}