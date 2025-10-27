import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
    google()
}

// 这里是 buildSrc 模块自身的依赖
// 我们需要 serialization 插件和 json 库来编译任务代码
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

