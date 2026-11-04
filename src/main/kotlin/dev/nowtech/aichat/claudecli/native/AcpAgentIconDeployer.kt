package dev.nowtech.aichat.claudecli.native

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.logger
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * JetBrains does not document icons in [acp.json] (see LLM-27862). Custom agents use a generic
 * robot icon unless `{system}/acp-agents/.icons/{sanitized-name}.svg` exists (PNG is ignored).
 */
object AcpAgentIconDeployer {

  private val LOG = logger<AcpAgentIconDeployer>()

  fun deployForAgent(displayName: String) {
    val slug = sanitizeAgentSlug(displayName)
    val iconsDir = PathManager.getSystemDir().resolve("acp-agents").resolve(".icons")
    Files.createDirectories(iconsDir)
    val svg = loadListIconSvg()
    writeIfChanged(iconsDir.resolve("$slug.svg"), svg)
    writeIfChanged(iconsDir.resolve("${slug}_dark.svg"), svg)
    // Older plugin builds wrote PNG; the IDE only loads SVG for custom agents.
    Files.deleteIfExists(iconsDir.resolve("$slug.png"))
    Files.deleteIfExists(iconsDir.resolve("${slug}_dark.png"))
    LOG.info("AI Chat Claude CLI Native: agent list icons -> $iconsDir/$slug.svg")
  }

  /** Same rules as [com.intellij.ml.llm.agents.acp.api.AcpAgentId.sanitize]. */
  fun sanitizeAgentSlug(displayName: String): String =
    displayName.lowercase(Locale.ROOT)
      .replace(Regex("[^a-z0-9]+"), "-")
      .trim('-')

  private fun loadListIconSvg(): String {
    val stream =
      AcpAgentIconDeployer::class.java.getResourceAsStream("/icons/claude-acp-list.svg")
        ?: error("Missing bundled /icons/claude-acp-list.svg")
    return stream.bufferedReader().readText()
  }

  private fun writeIfChanged(target: Path, content: String) {
    if (Files.isRegularFile(target) && Files.readString(target) == content) return
    Files.writeString(target, content)
  }
}
