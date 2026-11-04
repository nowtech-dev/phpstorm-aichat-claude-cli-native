package dev.nowtech.aichat.claudecli.native.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "AiChatClaudeCliNativeSettings", storages = [Storage("aiChatClaudeCliNative.xml")])
class NativeAcpSettings : PersistentStateComponent<NativeAcpSettings.State> {

  data class State(
    var autoInstallOnStartup: Boolean = true,
    var installRevision: Int = 0,
    var wslDistroId: String = "",
    var lastInstallError: String = "",
  )

  private var state = State()

  override fun getState(): State = state

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, this.state)
  }

  companion object {
    fun getInstance(): NativeAcpSettings =
      ApplicationManager.getApplication().getService(NativeAcpSettings::class.java)
  }
}
