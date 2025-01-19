package me.cdh

import me.cdh.Action.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.WindowConstants
import kotlin.enums.EnumEntries
import kotlin.math.abs
import kotlin.random.Random
import kotlin.system.exitProcess

val window = JFrame().apply {
    type = Window.Type.UTILITY
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    isUndecorated = true
    Dimension(100, 100).let {
        preferredSize = it
        minimumSize = it
    }
    setLocationRelativeTo(null)
    isAlwaysOnTop = true
    addMouseMotionListener(object : MouseAdapter() {
        override fun mouseDragged(e: MouseEvent?) {
            setLocation(
                e!!.locationOnScreen.x - width / 2, e.locationOnScreen.y - height / 2
            )
            if (changeAction(RISING)) frameNum = 0
        }
    })
    addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent?) {
            super.mouseReleased(e)
            if (action == RISING) {
                changeAction(LAYING)
                frameNum = 1
            }
        }

        override fun mouseClicked(e: MouseEvent?) {
            super.mouseClicked(e)
            bubbleState = BubbleState.HEART
            bubbleFrameNum = 0
        }
    })
    background = Color(1.0f, 1.0f, 1.0f, 0.0f)
    isVisible = true
    add(Kitty)
}
val frames = loadImg(entries)
val bubbleFrames = loadImg(BubbleState.entries)
var frameNum = 0
var action = SLEEP
var currFrames: List<BufferedImage>? = null
var layingDir = Direction.RIGHT
var state = State.DEFAULT
var wanderLoc = Point(0, 0)
var bubbleState = BubbleState.NONE
var currBubbleFrames: List<BufferedImage>? = null
var bubbleFrameNum = 0
var bubbleSteps = 0
var animationSteps = 0

fun <T> loadImg(entries: EnumEntries<T>): Map<String, List<BufferedImage>> where T : Enum<T>, T : Animation = buildMap {
    val catVarious = when (Random.nextInt(0, 4)) {
        0 -> "calico_cat"
        1 -> "grey_tabby_cat"
        2 -> "orange_cat"
        3 -> "white_cat"
        else -> throw IllegalArgumentException()
    }
    for (action in entries) {
        if (action.frame <= 0) continue
        val list = mutableListOf<BufferedImage>()
        this[action.name] = list
        val folderName = action.name.lowercase()
        for (i in 1..action.frame) {
            val inp = javaClass.classLoader.getResourceAsStream("$catVarious/$folderName/${folderName}_$i.png")
            list.add(ImageIO.read(inp))
        }
    }
}

fun tryWander() {
    if (Random.nextBoolean()) return
    state = State.WANDER
    val screenLoc = window.locationOnScreen
    var loc: Point
    do {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        loc = Point(
            Random.nextInt(screenSize.width - window.width - 20) + 10,
            Random.nextInt(screenSize.height - window.height - 20) + 10
        )
    } while (xyWithinThreshold(screenLoc, loc))
    wanderLoc = loc
}

fun xyWithinThreshold(px: Point, py: Point) = abs(px.y - py.y) <= 400 && abs(px.x - py.x) <= 400

fun flipImage(img: BufferedImage): BufferedImage {
    val mirror = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    mirror.createGraphics().run {
        transform(AffineTransform().apply {
            concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
            concatenate(AffineTransform.getTranslateInstance(-img.width.toDouble(), 0.0))
        })
        drawImage(img, 0, 0, null)
        dispose()
    }
    return mirror
}

fun changeAction(act: Action) = if (act != action) {
    action = act
    currFrames = frames[action.name]!!
    true
} else {
    false
}

fun doAction() {
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
        loc.x > screenSize.width - window.width ->
            loc.setLocation(screenSize.width - window.width, loc.y)

        loc.x < -10 ->
            loc.setLocation(-10, loc.y)

        loc.y > screenSize.height - window.height ->
            loc.setLocation(loc.x, screenSize.height - window.height)

        loc.y < -35 ->
            loc.setLocation(loc.x, -35)
    }
    window.location = loc
}

fun updateAnimation() {
    animationSteps++
    if (animationSteps >= action.delay) {
        when {
            action == LAYING && frameNum == action.frame - 1 -> {
                if ((animationSteps - action.delay) > 40) {
                    animationSteps = 0
                    frameNum = 0
                    if (Random.nextBoolean()) changeAction(CURLED)
                    else changeAction(SLEEP)
                }
            }

            action == SITTING && frameNum == action.frame - 1 -> {
                changeAction(LICKING)
                animationSteps = 0
                frameNum = 0
            }

            else -> {
                frameNum++
                animationSteps = 0
            }
        }
    }
    if (frameNum >= action.frame) frameNum = 0
}

fun bubbleState() {
    if (bubbleState != BubbleState.HEART) {
        if (action == SLEEP || action == CURLED) bubbleState = BubbleState.ZZZ
        else if (action != LICKING && action != SITTING) bubbleState = BubbleState.NONE
    }
    bubbleSteps++
    currBubbleFrames = bubbleFrames.getOrDefault(bubbleState.name, bubbleFrames[BubbleState.HEART.name])!!
    if (bubbleSteps >= bubbleState.delay) {
        bubbleFrameNum++
        bubbleSteps = 0
    }
    if (bubbleFrameNum >= bubbleState.frame) {
        bubbleFrameNum = 0
        if (bubbleState == BubbleState.HEART) bubbleState = BubbleState.NONE
    }
}


fun initSystemTray() {
    if (!SystemTray.isSupported()) return
    val trayIconSize = SystemTray.getSystemTray().trayIconSize
    val trayIcon = TrayIcon(
        ImageIO.read(Kitty.javaClass.classLoader.getResourceAsStream("kitty.png"))
            .getScaledInstance(trayIconSize.width, trayIconSize.height, Image.SCALE_SMOOTH),
        "Kitty"
    )
    trayIcon.popupMenu = PopupMenu().apply {
        val exit = MenuItem("Exit")
        exit.addActionListener { exitProcess(0) }
        add(exit)
    }
    SystemTray.getSystemTray().add(trayIcon)
}

fun updateAction() {
    if (action != RISING) {
        if (state == State.WANDER) {
            val curPos = window.locationOnScreen
            if (abs(curPos.x - wanderLoc.x) >= 3)
                if (curPos.x > wanderLoc.x)
                    changeAction(LEFT) else changeAction(RIGHT)
            else
                if (curPos.y > wanderLoc.y)
                    changeAction(UP) else changeAction(DOWN)
            if (wanderLoc.distance(curPos) < 3)
                state = State.DEFAULT
        }
        var changed = false
        when {
            action == LEFT -> layingDir = Direction.LEFT
            action == RIGHT -> layingDir = Direction.RIGHT
            state != State.WANDER && (action == UP || action == DOWN || action == LEFT || action == RIGHT) ->
                changed = if (Random.nextInt(3) >= 1) changeAction(LAYING) else changeAction(SITTING)

            else -> {}
        }
        if (changed) frameNum = 0
    }
}

fun isDayTime(): Boolean =
    DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now()).substring(0, 2).toInt() in 7..18