plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.20"
  id("org.jetbrains.intellij.platform") version "2.4.0"
}

group = "dev.nowtech"
version = "1.1.3"

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
    bundledPlugin("org.jetbrains.plugins.terminal")
  }

  val phpstormLib = file(phpstormHome).resolve("lib")
  compileOnly(
    files(
      phpstormLib.resolve("intellij.platform.ide.impl.jar"),
      phpstormLib.resolve("intellij.platform.projectModel.impl.jar"),
      phpstormLib.resolve("intellij.platform.vcs.impl.jar"),
    ),
  )

  implementation("com.google.code.gson:gson:2.11.0")
}

intellijPlatform {
  pluginConfiguration {
    id = "dev.nowtech.claude.cli.gui"
    name = "Claude CLI"
    version = project.version.toString()
    ideaVersion {
      sinceBuild = "262.10968"
      untilBuild = "262.*"
    }
    vendor {
      name = "NOW Tech"
      url = "https://nowtech.dev"
    }
  }
}

tasks {
  buildSearchableOptions {
    enabled = false
  }
  // Instrumentation scans every platform class — very slow on WSL + /mnt/c/.
  named("instrumentCode") { enabled = false }
  named("instrumentTestCode") { enabled = false }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
  }
}
