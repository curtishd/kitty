package me.cdh

enum class BubbleState(override val frame: Int, override val delay: Int) : Animation {
    DIZZY(4, 30),
    ZZZ(4, 30),
    HEART(4, 50),
    NONE(-1, -1)
}

enum class Action(override val frame: Int, override val delay: Int) : Animation {
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