package me.cdh

enum class BubbleState(override val delay: Int, override val frameRate: Int) : Animation {
    ZZZ(30, 4),
    HEART(50, 4),
    NONE(-1, -1)
}

enum class Action(override val frameRate: Int, override val delay: Int) : Animation {
    UP(4, 10),
    DOWN(4, 10),
    LEFT(4, 10),
    RIGHT(4, 10),
    CURLED(2, 40),
    LAYING(4, 20),
    SITTING(4, 20),

    LICKING(4, 40),
    RISING(2, 40),
    SLEEP(1, 10),
}
enum class State { DEFAULT, WANDER }
enum class Direction { RIGHT, LEFT }