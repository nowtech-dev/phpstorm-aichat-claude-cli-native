#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

usage() {
  cat <<'EOF'
Usage: ./builder <command>

Commands:
  build    Build the plugin (./gradlew buildPlugin)
  clean    Remove build artifacts
  help     Show this help
EOF
}

cmd="${1:-}"
case "$cmd" in
  build)
    ./gradlew buildPlugin --no-daemon
    zip_path="$(ls -t "$ROOT/build/distributions/"*.zip 2>/dev/null | head -1 || true)"
    if [[ -n "${zip_path:-}" && -f "$zip_path" ]]; then
      echo ""
      echo "Plugin ZIP: $zip_path"
    fi
    ;;
  clean)
    ./gradlew clean --no-daemon
    ;;
  help | -h | --help)
    usage
    ;;
  "")
    echo "Error: missing command." >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
  *)
    echo "Error: unknown command: $cmd" >&2
    echo "" >&2
    usage >&2
    exit 1
    ;;
esac
