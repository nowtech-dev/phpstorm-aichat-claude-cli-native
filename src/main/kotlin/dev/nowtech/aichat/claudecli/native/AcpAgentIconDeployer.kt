package dev.nowtech.aichat.claudecli.native

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * JetBrains does not document icons in [acp.json] (see LLM-27862). For custom agents the IDE
 * falls back to a generic robot icon unless it finds icons under
 * `{system}/acp-agents/.icons/{sanitized-name}.svg` or `.png` (and optional `{name}_dark.*`).
 */
object AcpAgentIconDeployer {

  private val LOG = logger<AcpAgentIconDeployer>()

  fun deployForAgent(displayName: String) {
    val slug = sanitizeAgentSlug(displayName)
    val iconsDir = PathManager.getSystemDir().resolve("acp-agents").resolve(".icons")
    Files.createDirectories(iconsDir)
    val png = loadPng()
    writeIfChanged(iconsDir.resolve("$slug.png"), png)
    writeIfChanged(iconsDir.resolve("${slug}_dark.png"), png)
    LOG.info("AI Chat Claude CLI Native: agent list icon -> $iconsDir/$slug.png")
  }

  /** Same rules as [com.intellij.ml.llm.agents.acp.api.AcpAgentId.sanitize]. */
  fun sanitizeAgentSlug(displayName: String): String =
    displayName.lowercase(Locale.ROOT)
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')

  private fun loadPng(): ByteArray {
    val stream =
      AcpAgentIconDeployer::class.java.getResourceAsStream("/icons/claude-acp.png")
        ?: error("Missing bundled /icons/claude-acp.png")
    return stream.readBytes()
  }

  private fun writeIfChanged(target: Path, content: ByteArray) {
    if (Files.isRegularFile(target) && Files.readAllBytes(target).contentEquals(content)) return
    Files.write(target, content)
  }
}
