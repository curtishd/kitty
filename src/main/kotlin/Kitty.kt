package me.cdh

import me.cdh.Action.*
import java.awt.Graphics
import javax.swing.JPanel

object Kitty : JPanel() {

    init {
        isOpaque = false
    }

    override fun paintComponent(g: Graphics?) {
        var cImg = currFrames[frameNum]
        if ((action == LAYING || action == RISING || action == SLEEP) && layingDir == Direction.LEFT || action == CURLED && layingDir == Direction.RIGHT)
            cImg = flipImage(cImg)
        g?.drawImage(cImg, 0, 0, 100, 100, null)
        if (bubbleState != BubbleState.NONE) {
            val currImg = currBubbleFrames[bubbleFrameNum]
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

    @Suppress
    private fun readResolve(): Any = Kitty
}