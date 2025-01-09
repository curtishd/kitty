package me.cdh

import me.cdh.Action.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.random.Random


enum class State { DEFAULT, WANDER }
enum class Direction { RIGHT, LEFT }

class Robot : JPanel() {
    private val frames: Map<String, List<BufferedImage>> = loadSprites(Action.entries.toTypedArray())
    private val bubbleFrames: Map<String, List<BufferedImage>> = loadSprites(BubbleState.entries.toTypedArray())
    val window: JFrame = JFrame()
    private val keyboardHandler: KeyBoardHandler = KeyBoardHandler()
    var frameNum = 0
    var action: Action = SLEEP
    private lateinit var currFrames: List<BufferedImage>
    private var layingDir = Direction.RIGHT
    private var state = State.DEFAULT
    private var wanderLoc = Point(0, 0)
    var bubbleState = BubbleState.NONE
    private lateinit var currBubbleFrames: List<BufferedImage>
    private var bubbleFrameNum = 0
    private var bubbleStep = 0

    private var animationSteps = AtomicInteger(0)

    init {
        isOpaque = false
        window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        window.isUndecorated = true
        window.type = Window.Type.UTILITY
        val dim = Dimension(100, 100)
        window.preferredSize = dim
        window.minimumSize = dim
        window.setLocationRelativeTo(null)
        window.isAlwaysOnTop = true
        window.isVisible = true
        window.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent?) {
                EventQueue.invokeLater {
                    window.setLocation(
                        e!!.locationOnScreen.x - window.width / 2, e.locationOnScreen.y - window.height / 2
                    )
                    if (changeAction(RISING)) {
                        frameNum = 0
                    }
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
        window.background = Color(1.0f, 1.0f, 1.0f, 1.0f)
        changeAction(CURLED)
        timer(initialDelay = 10L, period = 10L, action = {
            updateAction()
            doAction()
            updateAnimation()
            manageBubble()
            if (keyboardHandler.isPressed(KeyEvent.VK_W)) tryWander(true)
            window.repaint()
        })
        timer(initialDelay = 6000L, period = 6000L, action = { tryWander(false) })
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Robot()
        }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if ((action == LAYING || action == RISING || action == SLEEP) && layingDir == Direction.LEFT || action == CURLED && layingDir == Direction.RIGHT) {
            var currImg = currFrames[frameNum]
            currImg = flipImage(currImg)
            g?.drawImage(currImg, 0, 0, 100, 100, null)
        }
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
        } while (xyWithinThreshold(screenLoc, loc, 400))
        wanderLoc = loc
    }

    private fun <T> loadSprites(entries: Array<T>): Map<String, List<BufferedImage>> where T : Enum<T>, T : Animation {
        val map = HashMap<String, List<BufferedImage>>()
        for (action in entries) {
            if (action.frameRate <= 0) continue
            val list = arrayListOf<BufferedImage>()
            map[action.name] = list
            val folderName = action.name.lowercase()
            for (i in 1..action.frameRate) {
                val inputStream = javaClass.getResourceAsStream("/$folderName/${folderName}_$i.png")
                val resources = ImageIO.read(inputStream)
                list.add(resources)
            }
        }
        return map
    }

    private fun xyWithinThreshold(px: Point, py: Point, threshold: Int) =
        abs(px.y - py.y) <= threshold && abs(px.x - py.x) <= threshold

    private fun flipImage(img: BufferedImage): BufferedImage {
        val mirror = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
        val graph = mirror.createGraphics()
        val at = AffineTransform().apply {
            concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
            concatenate(AffineTransform.getTranslateInstance(-img.width.toDouble(), 0.0))
        }
        graph.transform(at)
        graph.drawImage(img, 0, 0, null)
        graph.dispose()
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

    fun changeAction(act: Action): Boolean {
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
        animationSteps.getAndUpdate { i -> i + 1 }
        if (animationSteps.get() >= action.delay) {
            if (action == LAYING && frameNum == action.frameRate - 1) {
                if ((animationSteps.get() - action.delay) > 40) {

                    animationSteps.set(0)
                    frameNum = 0
                    if (Random.nextBoolean()) changeAction(CURLED)
                    else changeAction(SLEEP)
                }
            } else if (action == SITTING && frameNum == action.frameRate - 1) {
                changeAction(LICKING)
                animationSteps.set(0)
                frameNum = 0
            } else {
                frameNum++
                animationSteps.set(0)
            }
        }
        if (frameNum >= action.frameRate) frameNum = 0
    }

    private fun manageBubble() {
        if (bubbleState != BubbleState.HEART) {
            if (action == SLEEP || action == CURLED) {
                bubbleState = BubbleState.ZZZ
            } else if (action != LICKING && action != SITTING) bubbleState = BubbleState.NONE
        }
        bubbleStep++
        currBubbleFrames = bubbleFrames.getOrDefault(bubbleState.name, bubbleFrames[BubbleState.HEART.name])!!
        if (bubbleStep >= bubbleState.delay) {
            bubbleFrameNum++
            bubbleStep = 0
        }
        if (bubbleFrameNum >= bubbleState.frameRate) {
            bubbleFrameNum = 0
            if (bubbleState == BubbleState.HEART) bubbleState = BubbleState.NONE
        }
    }
}