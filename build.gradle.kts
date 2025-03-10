plugins {
    kotlin("jvm") version "2.1.10"
}

group = "me.cdh"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
tasks.jar {
    manifest {
        attributes(
            mapOf("Main-Class" to "me.cdh.MainKt")
        )
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks {
    compileJava {
        dependsOn(compileKotlin)
        doFirst {
            options.compilerArgs = listOf(
                "--module-path", classpath.asPath
            )
        }
    }
    compileKotlin {
        destinationDirectory.set(compileJava.get().destinationDirectory)
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
task("copyDependencies", Copy::class) {
    configurations.compileClasspath.get()
        .filter { it.extension == "jar" }
        .forEach { from(it.absolutePath).into("${layout.buildDirectory.get()}/libs") }
}