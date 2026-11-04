package dev.nowtech.aichat.claudecli.native.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import dev.nowtech.aichat.claudecli.native.AcpInstaller
import javax.swing.JButton
import javax.swing.JComponent

class NativeAcpConfigurable : Configurable {

  private var autoInstallBox: JBCheckBox? = null
  private var statusLabel: JBLabel? = null

  override fun getDisplayName(): String = "AI Chat Claude CLI Native"

  override fun createComponent(): JComponent {
    val settings = NativeAcpSettings.getInstance()
    autoInstallBox = JBCheckBox("Install or repair on IDE startup", settings.state.autoInstallOnStartup)
    statusLabel = JBLabel(statusText(settings))
    val installButton =
      JButton("Install / repair Claude CLI agent now").apply {
        addActionListener {
          val open =
            com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
          AcpInstaller.installWithNotification(project = open, showSuccess = true)
        }
      }
    return FormBuilder.createFormBuilder()
      .addComponent(autoInstallBox!!)
      .addComponent(
        JBLabel(
          "<html>On Windows, the WSL distro is inferred from your <b>open project</b> " +
            "(path under <code>\\\\wsl.localhost\\…</code>). With multiple distros, open the WSL project before installing.</html>",
        ),
      )
      .addComponent(installButton)
      .addComponent(statusLabel!!)
      .panel
  }

  private fun statusText(settings: NativeAcpSettings): String {
    val err = settings.state.lastInstallError
    return if (err.isNotBlank()) {
      "Last error: ${err.take(400)}"
    } else {
      "Target: AI Chat → Agents → Claude CLI (Claude Code Teams via ACP, no JetBrains API key)."
    }
  }

  override fun isModified(): Boolean {
    val s = NativeAcpSettings.getInstance().state
    return autoInstallBox?.isSelected != s.autoInstallOnStartup
  }

  override fun apply() {
    val s = NativeAcpSettings.getInstance().state
    s.autoInstallOnStartup = autoInstallBox?.isSelected == true
    statusLabel?.text = statusText(NativeAcpSettings.getInstance())
  }

  override fun reset() {
    val s = NativeAcpSettings.getInstance().state
    autoInstallBox?.isSelected = s.autoInstallOnStartup
    statusLabel?.text = statusText(NativeAcpSettings.getInstance())
  }
}
