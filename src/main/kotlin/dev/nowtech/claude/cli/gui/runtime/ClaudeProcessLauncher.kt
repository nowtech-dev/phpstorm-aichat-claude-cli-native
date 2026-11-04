package dev.nowtech.claude.cli.gui.runtime

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.claudecode.settings.ClaudeSettings
import java.io.File

/**
 * Builds and starts a local [Process] for `claude -p …` with correct Windows / Linux / WSL handling.
 */
object ClaudeProcessLauncher {

  data class Launch(
    val command: List<String>,
    val hostWorkingDirectory: File,
  )

  fun prepare(project: Project, claudeArgv: List<String>): Launch {
    val spec = ClaudeEnvironmentDetector.detect(project)
    return when (spec.kind) {
      ClaudeEnvironmentKind.WINDOWS_NATIVE -> {
        Launch(claudeArgv, File(spec.workingDirectory))
      }

      ClaudeEnvironmentKind.WSL_WINDOWS_BRIDGE -> {
        val distro =
          spec.distribution
            ?: error("WSL distribution missing for bridge launch")
        val linuxExec = linuxSideExec(claudeArgv)
        Launch(
          WslLoginShell.bridgeArgv(distro, spec.workingDirectory, linuxExec),
          File(spec.processHostDirectory ?: windowsHostCwd()),
        )
      }

      ClaudeEnvironmentKind.WSL_PROJECT -> {
        if (SystemInfo.isWindows) {
          val distro =
            spec.distribution
              ?: error("WSL distribution missing for WSL project")
          val linuxExec = linuxSideExec(claudeArgv)
          Launch(
            WslLoginShell.bridgeArgv(distro, spec.workingDirectory, linuxExec),
            File(spec.processHostDirectory ?: windowsHostCwd()),
          )
        } else {
          Launch(wrapWithScriptIfNeeded(claudeArgv), File(spec.workingDirectory))
        }
      }

      ClaudeEnvironmentKind.LINUX_NATIVE -> {
        Launch(wrapWithScriptIfNeeded(claudeArgv), File(spec.workingDirectory))
      }
    }
  }

  /**
   * [claudeArgv] is built with a resolved CLI path; for WSL we replace the first token with the Linux command.
   */
  private fun linuxSideExec(claudeArgv: List<String>): List<String> {
    if (claudeArgv.isEmpty()) return listOf("claude")
    val configured =
      ClaudeSettings.getInstance().state.claudePath
        .ifBlank { com.claudecode.ClaudeConstants.DEFAULT_CLI_PATH }
        .trim()
        .split(Regex("\\s+"))
    return configured + claudeArgv.drop(1)
  }

  private fun wrapWithScriptIfNeeded(claudeArgv: List<String>): List<String> =
    when {
      SystemInfo.isWindows -> claudeArgv
      SystemInfo.isMac -> listOf("script", "-q", "/dev/null") + claudeArgv
      else ->
        listOf(
          "script",
          "-q",
          "-c",
          claudeArgv.joinToString(" ") { shellQuote(it) },
          "/dev/null",
        )
    }

  private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

  private fun windowsHostCwd(): String = System.getenv("USERPROFILE") ?: "C:\\"
}
