package me.cdh

import javax.swing.SwingUtilities
import kotlin.concurrent.timer

fun main() =
    SwingUtilities.invokeLater {
        initSystemTray()
        changeAction(Action.CURLED)
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