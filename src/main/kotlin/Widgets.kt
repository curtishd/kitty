package me.cdh

import java.awt.MenuItem
import java.awt.PopupMenu
import kotlin.system.exitProcess

object KittyPopupMenu : PopupMenu() {
    private val exit = MenuItem("Exit",).apply {
        addActionListener { exitProcess(0) }
    }

    init {
        add(exit)
    }

    private fun readResolve(): Any = KittyPopupMenu
}