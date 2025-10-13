plugins {
    kotlin("jvm") version "1.9.10" apply false
    kotlin("plugin.allopen") version "1.9.10" apply false
    id("io.quarkus") version "3.5.0" apply false
}

allprojects {
    group = "com.finance"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.quarkus")
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

    dependencies {
        val implementation by configurations
        val testImplementation by configurations

        implementation(platform("io.quarkus.platform:quarkus-bom:3.5.0"))
        implementation("io.quarkus:quarkus-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("io.quarkus:quarkus-arc")

        testImplementation("io.quarkus:quarkus-junit5")
        testImplementation("io.rest-assured:rest-assured")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        kotlinOptions.javaParameters = true
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Kotlin JVM toolchain pinned to Java 17
    extensions.configure(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java) {
        jvmToolchain(17)
    }
}
