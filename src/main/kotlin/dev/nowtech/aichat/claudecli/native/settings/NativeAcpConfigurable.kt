package dev.nowtech.aichat.claudecli.native.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import dev.nowtech.aichat.claudecli.native.AcpInstaller
import javax.swing.JButton
import javax.swing.JComponent

class NativeAcpConfigurable : Configurable {

  private var autoInstallBox: JBCheckBox? = null
  private var wslDistroField: JBTextField? = null
  private var statusLabel: JBLabel? = null

  override fun getDisplayName(): String = "Ai Chat Claude CLI Native"

  override fun createComponent(): JComponent {
    val settings = NativeAcpSettings.getInstance()
    autoInstallBox = JBCheckBox("Install or repair on IDE startup", settings.state.autoInstallOnStartup)
    wslDistroField = JBTextField(settings.state.wslDistroId, 20)
    statusLabel = JBLabel(statusText(settings))
    val installButton =
      JButton("Install / repair Claude CLI agent now").apply {
        addActionListener { AcpInstaller.installWithNotification(showSuccess = true) }
      }
    return FormBuilder.createFormBuilder()
      .addComponent(autoInstallBox!!)
      .addLabeledComponent("WSL distribution (Windows):", wslDistroField!!)
      .addComponent(
        JBLabel(
          "<html>Leave blank to use the first installed distro (e.g. <code>Ubuntu</code>). " +
            "Use this when PhpStorm opens projects under <code>\\\\wsl.localhost\\…</code>.</html>",
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
    return autoInstallBox?.isSelected != s.autoInstallOnStartup ||
      wslDistroField?.text?.trim() != s.wslDistroId.trim()
  }

  override fun apply() {
    val s = NativeAcpSettings.getInstance().state
    s.autoInstallOnStartup = autoInstallBox?.isSelected == true
    s.wslDistroId = wslDistroField?.text?.trim().orEmpty()
    statusLabel?.text = statusText(NativeAcpSettings.getInstance())
  }

  override fun reset() {
    val s = NativeAcpSettings.getInstance().state
    autoInstallBox?.isSelected = s.autoInstallOnStartup
    wslDistroField?.text = s.wslDistroId
    statusLabel?.text = statusText(NativeAcpSettings.getInstance())
  }
}
