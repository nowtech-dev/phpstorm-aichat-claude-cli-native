plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.20"
  id("org.jetbrains.intellij.platform") version "2.4.0"
}

group = "dev.nowtech"
version = "1.0.2"

kotlin {
  jvmToolchain(21)
}

val phpstormHome = providers.gradleProperty("phpstorm.home").get()

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    local(phpstormHome)
  }

  val phpstormLib = file(phpstormHome).resolve("lib")
  compileOnly(
    files(
      phpstormLib.resolve("intellij.platform.ide.impl.jar"),
    ),
  )

  implementation("com.google.code.gson:gson:2.11.0")
}

intellijPlatform {
  pluginConfiguration {
    id = "dev.nowtech.aichat.claudecli.native"
    name = "Ai Chat Claude CLI Native"
    version = project.version.toString()
    ideaVersion {
      sinceBuild = "262.10968"
      untilBuild = "262.*"
    }
    vendor {
      name = "NOW Tech"
      url = "https://jetbrains.nowtech.dev"
    }
  }
}

tasks {
  buildSearchableOptions {
    enabled = false
  }
  named("instrumentCode") { enabled = false }
  named("instrumentTestCode") { enabled = false }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
  }
}
