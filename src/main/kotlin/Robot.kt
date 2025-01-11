package me.cdh

import me.cdh.Action.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

object Robot : JPanel() {
    private val window = JFrame()
    private val keyboardHandler = KeyBoardHandler()
    private val frames: Map<String, List<BufferedImage>> = loadSprites(Action.entries.toTypedArray())
    private val bubbleFrames: Map<String, List<BufferedImage>> = loadSprites(BubbleState.entries.toTypedArray())
    private var frameNum = 0
    private var action: Action = SLEEP
    private var currFrames: List<BufferedImage>? = null
    private var layingDir = Direction.RIGHT
    private var state = State.DEFAULT
    private var wanderLoc = Point(0, 0)
    private var bubbleState = BubbleState.NONE
    private var currBubbleFrames: List<BufferedImage>? = null
    private var bubbleFrameNum = 0
    private var bubbleSteps = 0
    private var animationSteps = 0

    private var catVarious: String? = null

    init {
        window.type = Window.Type.UTILITY
        window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        window.isUndecorated = true
        val dim = Dimension(100, 100)
        window.preferredSize = dim
        window.minimumSize = dim
        window.setLocationRelativeTo(null)
        window.isAlwaysOnTop = true
        isOpaque = false
        window.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent?) {
                SwingUtilities.invokeLater {
                    window.setLocation(
                        e!!.locationOnScreen.x - window.width / 2, e.locationOnScreen.y - window.height / 2
                    )
                    if (changeAction(RISING)) frameNum = 0
                }
            }
        })
        window.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                super.mouseReleased(e)
                SwingUtilities.invokeLater {
                    if (action == RISING) {
                        changeAction(LAYING)
                        frameNum = 1
                    }
                }
            }

            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                bubbleState = BubbleState.HEART
                bubbleFrameNum = 0
            }
        })
        window.add(this)
        window.background = Color(1.0f, 1.0f, 1.0f, 0.0f)
        window.isVisible = true

        changeAction(CURLED)

        timer(initialDelay = 10L, period = 10L, action = {
            updateAction()
            doAction()
            updateAnimation()
            stateOfBubble()
            if (keyboardHandler.isPressed(KeyEvent.VK_W)) tryWander(true)
            window.repaint()
        })

        if (isDayTime()) timer(initialDelay = 30000L, period = 30000L, action = { tryWander(false) })
        else timer(initialDelay = 6000L, period = 6000L, action = { tryWander(false) })
        initSystemTray()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        var cImg = currFrames?.get(frameNum)
        if ((action == LAYING || action == RISING || action == SLEEP) && layingDir == Direction.LEFT || action == CURLED && layingDir == Direction.RIGHT) cImg =
            cImg?.let { flipImage(it) }
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

    private fun tryWander(force: Boolean) {
        if (!force && Random.nextBoolean()) return
        state = State.WANDER
        val screenLoc = window.locationOnScreen
        var loc: Point
        do {
            Toolkit.getDefaultToolkit().screenSize.let {
                loc = Point(
                    Random.nextInt(it.width - window.width - 20) + 10,
                    Random.nextInt(it.height - window.height - 20) + 10
                )
            }
        } while (xyWithinThreshold(screenLoc, loc))
        wanderLoc = loc
    }

    private fun <T> loadSprites(entries: Array<T>): Map<String, List<BufferedImage>> where T : Enum<T>, T : Animation =
        buildMap {
            catVarious = when (Random.nextInt(0, 4)) {
                0 -> "calico_cat"
                1 -> "grey_tabby_cat"
                2 -> "orange_cat"
                3 -> "white_cat"
                else -> throw IllegalArgumentException()
            }
            for (action in entries) {
                if (action.frameRate <= 0) continue
                val list = arrayListOf<BufferedImage>()
                this[action.name] = list
                val folderName = action.name.lowercase()
                for (i in 1..action.frameRate) {
                    val inp = javaClass.classLoader.getResourceAsStream("$catVarious/$folderName/${folderName}_$i.png")
                    requireNotNull(inp)
                    list.add(ImageIO.read(inp))
                }
            }
        }

    private fun xyWithinThreshold(px: Point, py: Point) = abs(px.y - py.y) <= 400 && abs(px.x - py.x) <= 400

    private fun flipImage(img: BufferedImage): BufferedImage {
        val mirror = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        val gTwoD = mirror.createGraphics()

        gTwoD.transform(AffineTransform().apply {
            concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
            concatenate(AffineTransform.getTranslateInstance(-img.width.toDouble(), 0.0))
        })
        gTwoD.drawImage(img, 0, 0, null)
        gTwoD.dispose()

        return mirror
    }

    private fun isMoveKeyPressed(): Boolean =
        keyboardHandler.isPressed(KeyEvent.VK_UP) || keyboardHandler.isPressed(KeyEvent.VK_DOWN) || keyboardHandler.isPressed(
            KeyEvent.VK_LEFT
        ) || keyboardHandler.isPressed(KeyEvent.VK_RIGHT)

    private fun updateAction() {
        if (action != RISING) {
            if (state == State.WANDER && !isMoveKeyPressed()) {
                val curPos = window.locationOnScreen
                if (abs(curPos.x - wanderLoc.x) >= 3) {
                    if (curPos.x > wanderLoc.x) changeAction(LEFT)
                    else changeAction(RIGHT)
                } else {
                    if (curPos.y > wanderLoc.y) changeAction(UP)
                    else changeAction(DOWN)
                }
                if (wanderLoc.distance(curPos) < 3) state = State.DEFAULT
            }
            var changed = false
            when {
                keyboardHandler.isPressed(KeyEvent.VK_UP) -> changed = changeAction(UP)
                keyboardHandler.isPressed(KeyEvent.VK_DOWN) -> changed = changeAction(DOWN)
                keyboardHandler.isPressed(KeyEvent.VK_LEFT) -> changed = changeAction(LEFT)
                keyboardHandler.isPressed(KeyEvent.VK_RIGHT) -> changed = changeAction(RIGHT)
                (action != CURLED && action != SITTING && action != LICKING && action != RISING && action != SLEEP && action != LAYING) -> {
                    if (action == LEFT) layingDir = Direction.LEFT
                    if (action == RIGHT) layingDir = Direction.RIGHT
                    if (state != State.WANDER) {
                        changed = if (Random.nextInt(3) >= 1) {
                            changeAction(LAYING)
                        } else {
                            changeAction(SITTING)
                        }
                    }
                }

                else -> {}
            }
            if (changed) frameNum = 0
        }
    }

    private fun changeAction(act: Action): Boolean {
        if (act != action) {
            action = act
            currFrames = frames[action.name]!!
            return true
        }
        return false
    }

    private fun doAction() {
        val loc = window.location
        when (action) {
            RIGHT -> loc.translate(1, 0)
            LEFT -> loc.translate(-1, 0)
            UP -> loc.translate(0, -1)
            DOWN -> loc.translate(0, 1)
            else -> {}
        }
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        when {
            loc.x > screenSize.width - window.width -> loc.setLocation(screenSize.width - window.width, loc.y)
            loc.x < -10 -> loc.setLocation(-10, loc.y)
            loc.y > screenSize.height - window.height -> loc.setLocation(loc.x, screenSize.height - window.height)
            loc.y < -35 -> loc.setLocation(loc.x, -35)
        }
        window.location = loc
    }

    private fun updateAnimation() {
        animationSteps++
        if (animationSteps >= action.delay) {
            if (action == LAYING && frameNum == action.frameRate - 1) {
                if ((animationSteps - action.delay) > 40) {

                    animationSteps = 0
                    frameNum = 0
                    if (Random.nextBoolean()) changeAction(CURLED)
                    else changeAction(SLEEP)
                }
            } else if (action == SITTING && frameNum == action.frameRate - 1) {
                changeAction(LICKING)
                animationSteps = 0
                frameNum = 0
            } else {
                frameNum++
                animationSteps = 0
            }
        }
        if (frameNum >= action.frameRate) frameNum = 0
    }

    private fun stateOfBubble() {
        if (bubbleState != BubbleState.HEART) {
            if (action == SLEEP || action == CURLED) {
                bubbleState = BubbleState.ZZZ
            } else if (action != LICKING && action != SITTING) bubbleState = BubbleState.NONE
        }
        bubbleSteps++
        currBubbleFrames = bubbleFrames.getOrDefault(bubbleState.name, bubbleFrames[BubbleState.HEART.name])!!
        if (bubbleSteps >= bubbleState.delay) {
            bubbleFrameNum++
            bubbleSteps = 0
        }
        if (bubbleFrameNum >= bubbleState.frameRate) {
            bubbleFrameNum = 0
            if (bubbleState == BubbleState.HEART) bubbleState = BubbleState.NONE
        }
    }

    private fun initSystemTray() {
        if (!SystemTray.isSupported()) return
        SwingUtilities.invokeLater {
            val img = ImageIO.read(javaClass.classLoader.getResourceAsStream("orange_cat/head.png"))
            val trayIconSize = SystemTray.getSystemTray().trayIconSize
            val trayImg = img.getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH)
            val trayIcon = TrayIcon(trayImg, "Kitty")
            val popupMenu = PopupMenu()
            val exit = MenuItem("Exit")
            exit.addActionListener { exitProcess(0) }
            popupMenu.add(exit)
            trayIcon.popupMenu = popupMenu
            SystemTray.getSystemTray().add(trayIcon)
        }
    }

    private fun readResolve(): Any = Robot
}

private fun isDayTime(): Boolean =
    DateTimeFormatter.ofPattern("hh:mm:ss").format(LocalDateTime.now()).substring(0, 2).toInt() in 7..18