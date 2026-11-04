package com.claudecode.ui

import com.claudecode.ui.theme.ChatTheme
import com.intellij.openapi.fileTypes.FileTypeManager
import java.io.File

/** HTML snippets styled like JetBrains AI Chat (bubbles + file cards). */
object AiChatBubbleHtml {

  fun userBubble(palette: ChatTheme.Palette, text: String): String {
    val body = escapeHtml(text)
    return """
      <div class="user-bubble-wrap">
        <div class="user-bubble">$body</div>
      </div>
    """.trimIndent()
  }

  fun assistantBlock(innerHtml: String): String =
    """<div class="assistant-block">$innerHtml</div>"""

  fun thoughtHeader(palette: ChatTheme.Palette, seconds: Int): String =
    """<div class="thought-header" style="color:${palette.fgMutedHex};">
      &#8250; Thought for ${seconds.coerceAtLeast(1)}s
    </div>"""

  fun fileCard(
    palette: ChatTheme.Palette,
    filePath: String,
    action: String? = null,
    meta: String? = null,
    elementId: String? = null,
    diffHref: String? = null,
  ): String {
    val file = File(filePath)
    val name = file.name
    val badge = fileTypeBadge(name)
    val idAttr = elementId?.let { " id='$it'" } ?: ""
    val actionLabel =
      when (action?.lowercase()) {
        "created", "write" -> "Created"
        "deleted" -> "Deleted"
        "modified", "edit" -> "Updated"
        null -> null
        else -> action
      }
    val metaHtml =
      when {
        meta != null -> """<span class="file-meta" style="color:${palette.fgMutedHex};">$meta</span>"""
        !actionLabel.isNullOrBlank() ->
          """<span class="file-meta" style="color:${palette.fgMutedHex};">$actionLabel</span>"""
        else -> ""
      }
    val diffLink =
      diffHref?.let {
        """ <a href="$it" style="color:${palette.linkHex};font-size:11px;">diff</a>"""
      } ?: ""
    return """
      <div class="file-card"$idAttr>
        <table cellpadding="0" cellspacing="0" width="100%"><tr>
          <td width="28" valign="middle">
            <span class="file-badge" style="background:${palette.surfaceHiHex};color:${palette.linkHex};">$badge</span>
          </td>
          <td valign="middle">
            <a href="file://$filePath" style="color:${palette.fgHex};text-decoration:none;font-weight:500;">${escapeHtml(name)}</a>
            $diffLink
          </td>
          <td align="right" valign="middle">$metaHtml</td>
        </tr></table>
      </div>
    """.trimIndent()
  }

  fun bashCard(palette: ChatTheme.Palette, command: String, copyHref: String): String {
    val cmd = escapeHtml(command.trim())
    return """
      <div class="cmd-card">
        <div class="cmd-label" style="color:${palette.fgMutedHex};">Terminal</div>
        <code class="cmd-text">$cmd</code>
        <a href="$copyHref" style="color:${palette.fgFaintHex};font-size:11px;">copy</a>
      </div>
    """.trimIndent()
  }

  fun fileListSummary(palette: ChatTheme.Palette, files: List<Pair<String, String>>): String {
    if (files.isEmpty()) return ""
    val cards =
      files.distinctBy { it.first }.joinToString("") { (path, action) ->
        val diff =
          if (action.equals("Modified", ignoreCase = true)) {
            "http://localhost/action/diff/$path"
          } else {
            null
          }
        fileCard(palette, path, action, diffHref = diff)
      }
    return """<div class="file-list">$cards</div>"""
  }

  private fun fileTypeBadge(fileName: String): String {
    val type = FileTypeManager.getInstance().getFileTypeByFileName(fileName)
    val fromType = type.name.take(3).uppercase()
    if (fromType.isNotBlank() && fromType != "TXT" && fromType != "UNK") {
      return fromType
    }
    return fileName.substringAfterLast('.', "?").take(4).uppercase()
  }

  private fun escapeHtml(text: String): String =
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("\n", "<br/>")
}
