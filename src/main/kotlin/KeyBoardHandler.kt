package me.cdh

import java.awt.KeyboardFocusManager
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import kotlin.system.exitProcess

class KeyBoardHandler : KeyAdapter() {
    private val pressedKeys = HashMap<Int, Boolean>()

    init {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            synchronized(this.javaClass) {
                when (event.id) {
                    KeyEvent.KEY_PRESSED -> {
                        pressedKeys[event.keyCode] = true
                        if (event.keyCode == KeyEvent.VK_ESCAPE) exitProcess(0)
                    }

                    KeyEvent.KEY_RELEASED -> {
                        pressedKeys[event.keyCode] = false
                    }
                }
                return@synchronized false
            }
        }
    }

    fun isPressed(keyCode: Int) = pressedKeys.getOrDefault(keyCode, false)
}