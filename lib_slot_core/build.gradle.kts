plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("com.books_goo_hzz.lib_slot_core.GeneratorKt")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sqlite.jdbc)
    testImplementation(libs.junit)
}
