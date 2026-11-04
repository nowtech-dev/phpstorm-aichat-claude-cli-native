package dev.nowtech.aichat.claudecli.native

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class InstallNativeAcpAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    AcpInstaller.installWithNotification(showSuccess = true)
  }
}
