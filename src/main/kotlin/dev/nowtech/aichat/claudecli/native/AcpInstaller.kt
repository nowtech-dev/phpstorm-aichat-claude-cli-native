package dev.nowtech.aichat.claudecli.native

import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfo
import dev.nowtech.aichat.claudecli.native.settings.NativeAcpSettings
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import java.util.EnumSet
import java.util.concurrent.TimeUnit

object AcpInstaller {

  private val LOG = logger<AcpInstaller>()

  const val INSTALL_REVISION = 3
  const val ACP_PACKAGE = "@agentclientprotocol/claude-agent-acp@0.99.0"
  const val NOTIFICATION_GROUP = "Ai Chat Claude CLI Native"

  fun installWithNotification(showSuccess: Boolean = true) {
    ApplicationManager.getApplication().executeOnPooledThread {
      val result = runCatching { install() }
      ApplicationManager.getApplication().invokeLater {
        val settings = NativeAcpSettings.getInstance()
        result.fold(
          onSuccess = { summary ->
            settings.state.installRevision = INSTALL_REVISION
            settings.state.lastInstallError = ""
            LOG.info("AI Chat Claude CLI Native: install ok")
            if (showSuccess) {
              notify("Claude CLI agent installed", summary, NotificationType.INFORMATION)
            }
          },
          onFailure = { err ->
            val msg = err.message ?: err.toString()
            settings.state.lastInstallError = msg
            LOG.warn("AI Chat Claude CLI Native: install failed", err)
            notify("Claude CLI agent install failed", msg, NotificationType.ERROR)
          },
        )
      }
    }
  }

  fun needsInstall(): Boolean {
    val s = NativeAcpSettings.getInstance().state
    if (!s.autoInstallOnStartup) return false
    if (s.installRevision < INSTALL_REVISION) return true
    return !isRuntimePresent(resolveWslDistributionOrNull())
  }

  fun install(): String {
    return when {
      SystemInfo.isWindows -> installWindows()
      SystemInfo.isLinux || SystemInfo.isMac -> installNativeUnix()
      else -> error("Unsupported OS for ACP install")
    }
  }

  private fun installWindows(): String {
    val distro = resolveWslDistribution()
    val linuxHome = queryWslHome(distro)
    val winHome = System.getenv("USERPROFILE") ?: error("USERPROFILE not set")
    val acpJson = Paths.get(winHome, ".jetbrains", "acp.json")

    AcpJsonMerger.mergeAgent(acpJson, linuxHome)
    deployLauncherToWsl(distro, linuxHome)
    npmInstallRuntime(distro)
    AcpAgentIconDeployer.deployForAgent(AcpJsonMerger.AGENT_NAME)

    return buildString {
      appendLine("WSL (${distro.msId}): $linuxHome/.local/bin/claude-teams-acp")
      appendLine("Runtime: ~/.local/share/claude-teams-acp ($ACP_PACKAGE)")
      appendLine("Config: $acpJson")
      append("In AI Chat, choose Agents → \"${AcpJsonMerger.AGENT_NAME}\".")
    }
  }

  private fun installNativeUnix(): String {
    val home = System.getenv("HOME") ?: error("HOME not set")
    val acpJson = Paths.get(home, ".jetbrains", "acp.json")
    AcpJsonMerger.mergeAgent(acpJson, home)
    deployLauncherLocal(home)
    npmInstallLocal(home)
    AcpAgentIconDeployer.deployForAgent(AcpJsonMerger.AGENT_NAME)
    return "Installed AI Chat agent at $home/.jetbrains/acp.json"
  }

  private fun deployLauncherLocal(home: String) {
    val binDir = Paths.get(home, ".local", "bin")
    Files.createDirectories(binDir)
    val target = binDir.resolve("claude-teams-acp")
    Files.writeString(target, loadLauncherScript())
    setExecutableUnix(target)
  }

  private fun deployLauncherToWsl(distro: WSLDistribution, linuxHome: String) {
    val wslHome = wslUncHome(distro) ?: error("Could not resolve WSL home for ${distro.msId}")
    val binDir = wslHome.resolve(".local").resolve("bin")
    Files.createDirectories(binDir)
    val target = binDir.resolve("claude-teams-acp")
    Files.writeString(target, loadLauncherScript())
    runWsl(distro, listOf("chmod", "+x", "$linuxHome/.local/bin/claude-teams-acp"), timeoutSec = 30)
  }

  private fun npmInstallLocal(home: String) {
    val prefix = Paths.get(home, ".local", "share", "claude-teams-acp")
    if (Files.isRegularFile(prefix.resolve("node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js"))) {
      return
    }
    Files.createDirectories(prefix)
    runProcess(
      listOf(
        "npm",
        "install",
        "--prefix",
        prefix.toString(),
        ACP_PACKAGE,
        "--omit=dev",
        "--no-fund",
        "--no-audit",
      ),
      timeoutSec = 600,
    )
  }

  private fun npmInstallRuntime(distro: WSLDistribution) {
    val marker = "node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js"
    val check = runWsl(distro, listOf("bash", "-lic", "test -f ~/.local/share/claude-teams-acp/$marker"), timeoutSec = 30)
    if (check == 0) return

    val script =
      """
      set -euo pipefail
      mkdir -p "${'$'}HOME/.local/share/claude-teams-acp"
      npm install --prefix "${'$'}HOME/.local/share/claude-teams-acp" $ACP_PACKAGE --omit=dev --no-fund --no-audit
      """.trimIndent()
    val code = runWsl(distro, listOf("bash", "-lic", script), timeoutSec = 600)
    if (code != 0) error("npm install in WSL exited with code $code")
  }

  fun isRuntimePresent(distro: WSLDistribution?): Boolean {
    return when {
      SystemInfo.isWindows && distro != null -> {
        runWsl(
          distro,
          listOf(
            "bash",
            "-lic",
            "test -x ~/.local/bin/claude-teams-acp && test -f ~/.local/share/claude-teams-acp/node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js",
          ),
          timeoutSec = 45,
        ) == 0
      }
      SystemInfo.isLinux || SystemInfo.isMac -> {
        val home = System.getenv("HOME") ?: return false
        Files.isExecutable(Paths.get(home, ".local", "bin", "claude-teams-acp")) &&
          Files.isRegularFile(
            Paths.get(
              home,
              ".local/share/claude-teams-acp/node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js",
            ),
          )
      }
      else -> false
    }
  }

  private fun loadLauncherScript(): String {
    val stream =
      AcpInstaller::class.java.getResourceAsStream("/acp/claude-teams-acp.sh")
        ?: error("Missing bundled /acp/claude-teams-acp.sh")
    return stream.bufferedReader().readText()
  }

  private fun resolveWslDistributionOrNull(): WSLDistribution? =
    runCatching { resolveWslDistribution() }.getOrNull()

  private fun resolveWslDistribution(): WSLDistribution {
    val preferred = NativeAcpSettings.getInstance().state.wslDistroId.trim()
    val installed = WslDistributionManager.getInstance().installedDistributions
    if (preferred.isNotEmpty()) {
      installed.firstOrNull { it.msId.equals(preferred, ignoreCase = true) }
        ?.let { return it }
    }
    return installed.firstOrNull()
      ?: error("No WSL distribution found. Install WSL or set the distro in Settings.")
  }

  private fun wslUncHome(distro: WSLDistribution): Path? {
    val raw = distro.userHome ?: return null
    return Paths.get(raw)
  }

  private fun queryWslHome(distro: WSLDistribution): String {
    val pb =
      ProcessBuilder(
        WSLDistribution.findWslExe().toString(),
        WSLDistribution.DISTRIBUTION_PARAMETER,
        distro.msId,
        "--",
        "bash",
        "-lic",
        "printf '%s' \"\$HOME\"",
      )
    val out = runProcess(pb, timeoutSec = 60).trim()
    if (out.isEmpty()) error("Could not read HOME from WSL (${distro.msId})")
    return out
  }

  private fun runWsl(distro: WSLDistribution, linuxArgv: List<String>, timeoutSec: Long): Int {
    val pb =
      ProcessBuilder(
        buildList {
          add(WSLDistribution.findWslExe().toString())
          add(WSLDistribution.DISTRIBUTION_PARAMETER)
          add(distro.msId)
          add("--")
          addAll(linuxArgv)
        },
      )
    return runProcessExitCode(pb, timeoutSec)
  }

  private fun runProcess(argv: List<String>, timeoutSec: Long): String {
    val pb = ProcessBuilder(argv)
    return runProcess(pb, timeoutSec)
  }

  private fun runProcess(pb: ProcessBuilder, timeoutSec: Long): String {
    pb.redirectErrorStream(true)
    val p = pb.start()
    val out = p.inputStream.bufferedReader().readText()
    if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
      p.destroyForcibly()
      error("Command timed out after ${timeoutSec}s: ${pb.command().joinToString(" ")}")
    }
    if (p.exitValue() != 0) {
      error("Command failed (${p.exitValue()}): ${pb.command().joinToString(" ")}\n$out")
    }
    return out
  }

  private fun runProcessExitCode(pb: ProcessBuilder, timeoutSec: Long): Int {
    pb.redirectErrorStream(true)
    val p = pb.start()
    p.inputStream.transferTo(java.io.OutputStream.nullOutputStream())
    if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {
      p.destroyForcibly()
      return -1
    }
    return p.exitValue()
  }

  private fun setExecutableUnix(path: Path) {
    runCatching {
      val perms = Files.getPosixFilePermissions(path)
      perms.addAll(
        EnumSet.of(
          PosixFilePermission.OWNER_EXECUTE,
          PosixFilePermission.GROUP_EXECUTE,
          PosixFilePermission.OTHERS_EXECUTE,
        ),
      )
      Files.setPosixFilePermissions(path, perms)
    }
  }

  private fun notify(title: String, content: String, type: NotificationType) {
    Notifications.Bus.notify(Notification(NOTIFICATION_GROUP, title, content, type))
  }
}
