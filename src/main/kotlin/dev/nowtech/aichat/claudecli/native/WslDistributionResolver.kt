package dev.nowtech.aichat.claudecli.native

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.execution.wsl.WslPath
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo

object WslDistributionResolver {

  /**
   * Picks the WSL distro where the Claude CLI agent should be installed.
   * Uses the opened project path (UNC `\\wsl.localhost\…` or Eel-backed WSL) when available.
   */
  fun resolve(project: Project? = null): WSLDistribution {
    if (!SystemInfo.isWindows) {
      error("WSL resolution is only used on Windows")
    }
    val installed = WslDistributionManager.getInstance().installedDistributions
    if (installed.isEmpty()) {
      error("No WSL distribution found. Install WSL or open the project from a WSL path.")
    }

    val candidates = LinkedHashSet<Project>()
    project?.let { candidates.add(it) }
    candidates.addAll(ProjectManager.getInstance().openProjects)

    for (p in candidates) {
      distributionForProject(p)?.let { return it }
    }

    if (installed.size == 1) {
      return installed.single()
    }

    error(
      "Several WSL distros are installed (${installed.joinToString { it.msId }}). " +
        "Open the project from \\\\wsl.localhost\\… so the IDE can pick the right one.",
    )
  }

  fun resolveOrNull(project: Project? = null): WSLDistribution? =
    runCatching { resolve(project) }.getOrNull()

  private fun distributionForProject(project: Project): WSLDistribution? {
    val basePath = project.basePath ?: return null
    if (WslPath.isWslUncPath(basePath)) {
      return WslPath.parseWindowsUncPath(basePath)?.distribution
    }
    return null
  }
}
