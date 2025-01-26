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

val frames = loadImg(entries) // 加载猫的图片
var frameNum = 0 // 当前动画帧索引
var action = SLEEP // 当前行为
var currFrames: List<BufferedImage>? = null // 当前猫动画帧
var animationSteps = 0 // 猫的动画播放速度
var state = State.DEFAULT // 猫的行为
var layingDir = Direction.RIGHT // 趴下的方向
var wanderLoc = Point(0, 0) // 尝试游走的方向
var mood = 30 // 心情

val bubbleFrames = loadImg(BubbleState.entries) // 加载气泡图片
var bubbleState = BubbleState.NONE // 气泡状态
var currBubbleFrames: List<BufferedImage>? = null // 当前气泡动画状态
var bubbleFrameNum = 0 // 气泡状态的动画帧索引
var bubbleSteps = 0 // 气泡动画的播放速度

// -------------------------------------------------------------------------------------
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
    isResizable = false
    addMouseMotionListener(object : MouseAdapter() {
        override fun mouseDragged(e: MouseEvent?) {
            setLocation(e!!.locationOnScreen.x - width / 2, e.locationOnScreen.y - height / 2)
            if (changeAction(RISING)) frameNum = 0
        }
    })
    addMouseListener(object : MouseAdapter() {
        override fun mouseReleased(e: MouseEvent?) {
            if (action == RISING) {
                changeAction(LAYING)
                frameNum = 1
            }
        }

        override fun mouseClicked(e: MouseEvent?) {
            if (mood < 110) mood += 10
            bubbleState = if (mood > 50) {
                BubbleState.HEART
            } else {
                BubbleState.DIZZY
            }
            bubbleFrameNum = 0
        }
    })
    background = Color(1.0f, 1.0f, 1.0f, 0.0f)
    isVisible = true
    add(Kitty)
}

// 加载图片
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
            list.add(ImageIO.read(javaClass.classLoader.getResourceAsStream("$catVarious/$folderName/${folderName}_$i.png")))
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

fun xyWithinThreshold(px: Point, py: Point): Boolean = abs(px.y - py.y) <= 400 && abs(px.x - py.x) <= 400

fun flipImage(img: BufferedImage): BufferedImage {
    val mirror = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    val gTwoD = mirror.createGraphics()
    val afTransform = AffineTransform()
    afTransform.concatenate(AffineTransform.getScaleInstance(-1.0, 1.0))
    afTransform.concatenate(AffineTransform.getTranslateInstance(-img.width.toDouble(), 0.0))
    gTwoD.transform(afTransform)
    gTwoD.drawImage(img, 0, 0, null)
    gTwoD.dispose()
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
                    if (Random.nextBoolean()) changeAction(CURLED) else changeAction(SLEEP)
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
    bubbleSteps++
    currBubbleFrames = if (mood > 50) {
        bubbleFrames.getOrDefault(bubbleState.name, bubbleFrames[BubbleState.HEART.name])
    } else {
        bubbleFrames.getOrDefault(bubbleState.name, bubbleFrames[BubbleState.DIZZY.name])
    }
    // 控制播放速度
    if (bubbleSteps >= bubbleState.delay) {
        bubbleFrameNum++
        bubbleSteps = 0
    }
    // 控制心情气泡的持续时间
    if (action != CURLED && action != LAYING) bubbleState = BubbleState.NONE
    if (bubbleFrameNum >= bubbleState.frame) {
        when {
            action == CURLED || action == LAYING -> bubbleState = BubbleState.ZZZ
            action != LICKING && action != SITTING -> bubbleState = BubbleState.NONE
        }
        bubbleFrameNum = 0
        if (bubbleState == BubbleState.HEART || bubbleState == BubbleState.DIZZY) {
            bubbleState = BubbleState.NONE
        }
    }
}

// 初始化系统托盘
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

// 更新行为
fun updateAction() {
    if (action != RISING) {
        if (state == State.WANDER) {
            val curPos = window.locationOnScreen
            when {
                abs(curPos.x - wanderLoc.x) >= 3 ->
                    if (curPos.x > wanderLoc.x) changeAction(LEFT) else changeAction(RIGHT)

                else ->
                    if (curPos.y > wanderLoc.y) changeAction(UP) else changeAction(DOWN)
            }
            if (wanderLoc.distance(curPos) < 3) state = State.DEFAULT
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