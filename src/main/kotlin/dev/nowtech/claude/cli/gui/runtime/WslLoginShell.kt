package dev.nowtech.claude.cli.gui.runtime

import com.intellij.execution.wsl.WSLDistribution

/**
 * Wraps CLI spawns on Windows→WSL so login + interactive bash sources nvm/fnm and
 * `~/.local/bin` (same approach as Swttch's WslPathResolver / issue #57).
 */
object WslLoginShell {

  fun bridgeArgv(
    distribution: WSLDistribution,
    linuxCwd: String,
    execArgv: List<String>,
  ): List<String> {
    val wslExe = WSLDistribution.findWslExe().toString()
    val snippet = "exec ${execArgv.joinToString(" ") { shellQuote(it) }}"
    // Same shape as Swttch WslPathResolver: `wsl … --cd … -- bash -lic "exec …"`.
    // Do NOT pass WSLDistribution.EXEC_PARAMETER (`--exec`) here — adding `--` after
    // it makes WSL try to execvpe("--") and fail.
    return buildList {
      add(wslExe)
      add(WSLDistribution.DISTRIBUTION_PARAMETER)
      add(distribution.msId)
      add("--cd")
      add(linuxCwd)
      add("--")
      add("bash")
      add("-lic")
      add(snippet)
    }
  }

  /** Run inside an already-WSL terminal session (JetBrains WSL Eel). */
  fun inDistroExecArgv(execArgv: List<String>): List<String> {
    val snippet = "exec ${execArgv.joinToString(" ") { shellQuote(it) }}"
    return listOf("bash", "-lic", snippet)
  }

  private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
