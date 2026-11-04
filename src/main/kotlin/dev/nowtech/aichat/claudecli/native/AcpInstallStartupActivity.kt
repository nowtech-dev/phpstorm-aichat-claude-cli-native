package dev.nowtech.aichat.claudecli.native

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class AcpInstallStartupActivity : StartupActivity {

  private val LOG = logger<AcpInstallStartupActivity>()

  override fun runActivity(project: Project) {
    if (!AcpInstaller.needsInstall()) return
    synchronized(InstallGate) {
      if (InstallGate.done) return
      InstallGate.done = true
    }
    LOG.info("AI Chat Claude CLI Native: scheduling ACP install")
    AcpInstaller.installWithNotification(showSuccess = false)
  }

  private object InstallGate {
    @Volatile var done = false
  }
}
