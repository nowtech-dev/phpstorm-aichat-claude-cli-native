package dev.nowtech.claude.cli.gui.runtime

import com.intellij.execution.wsl.WSLDistribution

data class ClaudeLaunchSpec(
  val kind: ClaudeEnvironmentKind,
  val description: String,
  val shellCommand: List<String>,
  /** Directory passed to Claude (Linux path when the project is on WSL). */
  val workingDirectory: String,
  /** Host OS directory for [ProcessBuilder.directory] when bridging via wsl.exe. */
  val processHostDirectory: String? = null,
  val distribution: WSLDistribution? = null,
  val extraEnv: Map<String, String> = emptyMap(),
)
