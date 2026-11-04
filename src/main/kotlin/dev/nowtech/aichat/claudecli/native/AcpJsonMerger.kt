package dev.nowtech.aichat.claudecli.native

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object AcpJsonMerger {

  const val AGENT_NAME = "Claude CLI"

  private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

  fun mergeAgent(target: Path, linuxHome: String) {
    Files.createDirectories(target.parent)
    val root =
      if (Files.isRegularFile(target)) {
        runCatching { JsonParser.parseString(Files.readString(target)).asJsonObject }
          .getOrElse { JsonObject() }
      } else {
        JsonObject()
      }

    if (!root.has("default_mcp_settings")) {
      root.add(
        "default_mcp_settings",
        JsonObject().apply {
          addProperty("use_custom_mcp", true)
          addProperty("use_idea_mcp", false)
        },
      )
    }

    val servers =
      root.getAsJsonObject("agent_servers")
        ?: JsonObject().also { root.add("agent_servers", it) }

    servers.add(
      AGENT_NAME,
      JsonObject().apply {
        addProperty("command", "$linuxHome/.local/bin/claude-teams-acp")
        add("args", com.google.gson.JsonArray())
        add(
          "env",
          JsonObject().apply {
            addProperty(
              "PATH",
              "$linuxHome/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            )
          },
        )
      },
    )

    val json = gson.toJson(root)
    if (Files.isRegularFile(target)) {
      val backup = target.resolveSibling("${target.fileName}.bak.${System.currentTimeMillis()}")
      Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING)
    }
    Files.writeString(target, json)
  }
}
