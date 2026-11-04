package dev.nowtech.claude.cli.gui.runtime

import com.claudecode.ClaudeConstants
import com.claudecode.settings.ClaudeSettings
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.eel.provider.EelProviderUtil
import com.intellij.platform.eel.provider.LocalEelDescriptor
import java.nio.file.Files
import java.nio.file.Paths

object ClaudeEnvironmentDetector {

  fun detect(project: Project): ClaudeLaunchSpec {
    val settings = ClaudeSettings.getInstance().state
    val basePath = project.basePath
    val linuxClaude = splitCommand(settings.claudePath.ifBlank { ClaudeConstants.DEFAULT_CLI_PATH })
    val extraArgs = modelArgs(settings.model)

    if (SystemInfo.isWindows && isWslBackedProject(project)) {
      val distribution = resolveDistribution(basePath)
      val cwd = linuxWorkingDirectory(distribution, basePath)
      return ClaudeLaunchSpec(
        kind = ClaudeEnvironmentKind.WSL_PROJECT,
        description = "WSL (${distribution.msId})",
        shellCommand = WslLoginShell.inDistroExecArgv(linuxClaude + extraArgs),
        workingDirectory = cwd,
        processHostDirectory = windowsHostWorkingDirectory(),
        distribution = distribution,
      )
    }

    if (SystemInfo.isWindows && basePath != null && WslPath.isWslUncPath(basePath)) {
      val wslPath =
        WslPath.parseWindowsUncPath(basePath)
          ?: error("Could not parse WSL path: $basePath")
      val distribution = wslPath.distribution
      return ClaudeLaunchSpec(
        kind = ClaudeEnvironmentKind.WSL_WINDOWS_BRIDGE,
        description = "WSL via ${distribution.msId}",
        shellCommand =
          WslLoginShell.bridgeArgv(distribution, wslPath.linuxPath, linuxClaude + extraArgs),
        workingDirectory = wslPath.linuxPath,
        processHostDirectory = windowsHostWorkingDirectory(),
        distribution = distribution,
      )
    }

    if (SystemInfo.isWindows) {
      return ClaudeLaunchSpec(
        kind = ClaudeEnvironmentKind.WINDOWS_NATIVE,
        description = "Windows",
        shellCommand = splitCommand(settings.claudePath.ifBlank { "claude.exe" }) + extraArgs,
        workingDirectory = basePath ?: System.getenv("USERPROFILE") ?: "C:\\",
      )
    }

    val linuxCwd = basePath ?: System.getenv("HOME") ?: "/"
    val label =
      when {
        SystemInfo.isMac -> "macOS"
        isIdeRunningInsideWsl() -> "Linux (WSL)"
        else -> "Linux"
      }
    return ClaudeLaunchSpec(
      kind = ClaudeEnvironmentKind.LINUX_NATIVE,
      description = label,
      shellCommand = linuxClaude + extraArgs,
      workingDirectory = linuxCwd,
    )
  }

  private fun modelArgs(model: String): List<String> {
    val id = model.trim()
    return if (id.isEmpty()) emptyList() else listOf("--model", id)
  }

  private fun splitCommand(raw: String): List<String> =
    raw.trim().takeIf { it.isNotEmpty() }?.split(Regex("\\s+")) ?: listOf("claude")

  private fun isWslBackedProject(project: Project): Boolean {
    if (!SystemInfo.isWindows) {
      return false
    }
    return EelProviderUtil.getEelDescriptor(project) !is LocalEelDescriptor
  }

  private fun resolveDistribution(basePath: String?): WSLDistribution {
    if (basePath != null && WslPath.isWslUncPath(basePath)) {
      WslPath.parseWindowsUncPath(basePath)?.distribution?.let { return it }
    }
    val installed = WslDistributionManager.getInstance().installedDistributions
    return installed.firstOrNull()
      ?: error("No WSL distribution found. Install WSL or open the project from a WSL path.")
  }

  private fun linuxWorkingDirectory(distribution: WSLDistribution, basePath: String?): String {
    if (basePath != null && WslPath.isWslUncPath(basePath)) {
      return WslPath.parseWindowsUncPath(basePath)?.linuxPath
        ?: linuxHome(distribution)
    }
    if (basePath != null && basePath.startsWith("/")) {
      return basePath
    }
    if (basePath != null && !basePath.startsWith("\\\\")) {
      return distribution.getWslPath(basePath)?.replace('\\', '/') ?: linuxHome(distribution)
    }
    return linuxHome(distribution)
  }

  private fun linuxHome(distribution: WSLDistribution): String =
    distribution.userHome?.replace('\\', '/') ?: "/"

  private fun windowsHostWorkingDirectory(): String =
    System.getenv("USERPROFILE") ?: "C:\\"

  private fun isIdeRunningInsideWsl(): Boolean {
    if (System.getenv("WSL_DISTRO_NAME")?.isNotBlank() == true) {
      return true
    }
    return runCatching {
      val version = Files.readString(Paths.get("/proc/version"))
      version.contains("microsoft", ignoreCase = true) || version.contains("WSL", ignoreCase = true)
    }.getOrDefault(false)
  }
}
