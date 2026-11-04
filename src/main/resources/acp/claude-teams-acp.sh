#!/usr/bin/env bash
# Claude Code Teams → AI Chat (ACP). Installed by the Claude CLI plugin.
set -euo pipefail

export PATH="${PATH:-${HOME}/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin}"

INSTALL_DIR="${CLAUDE_TEAMS_ACP_HOME:-${HOME}/.local/share/claude-teams-acp}"
ENTRY="${INSTALL_DIR}/node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js"

if [[ ! -f "$ENTRY" ]]; then
  JB_ENTRY="$(ls -d "${HOME}/.cache/JetBrains"/PhpStorm*/acp-agents/claude-acp/0.99.0/node_modules/@agentclientprotocol/claude-agent-acp/dist/index.js 2>/dev/null | head -1 || true)"
  if [[ -n "$JB_ENTRY" && -f "$JB_ENTRY" ]]; then
    ENTRY="$JB_ENTRY"
  fi
fi

if [[ ! -f "$ENTRY" ]]; then
  echo "Claude Teams ACP runtime missing. In PhpStorm: Settings → Tools → Claude Code → Install AI Chat agent." >&2
  exit 1
fi

NODE_BIN="${CLAUDE_TEAMS_NODE:-$(command -v node || true)}"
if [[ -z "$NODE_BIN" || ! -x "$NODE_BIN" ]]; then
  echo "node not found in PATH" >&2
  exit 1
fi

exec "$NODE_BIN" "$ENTRY" "$@"
