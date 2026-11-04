# Claude CLI (PhpStorm plugin)

Painel **nativo** no PhpStorm que conversa com o **Claude Code** só via CLI (`claude -p`, JSON stream) — **sem Node embutido**, sem backend Swttch.

Baseado em [claude-code-intellij](https://github.com/BHRother/claude-code-intellij) (Apache 2.0 — ver `NOTICE` e `LICENSE-claude-code-intellij`) com camada **WSL** própria (`dev.nowtech.claude.cli.gui.runtime`).

## Funcionalidades

- Tool window **Claude Code** com chat, markdown, ferramentas, seletor de modelo, sessões múltiplas
- Atalho **Ctrl+Shift+I** (Toggle)
- Menu de contexto (editor, project view, console)
- Projeto WSL no Windows: `wsl.exe` + `bash -lic` para achar `claude` em `~/.local/bin` / nvm

## Build

```bash
cp gradle.properties.example gradle.properties   # ajuste phpstorm.home
./gradlew buildPlugin --console=plain
```

**WSL + PhpStorm em `/mnt/c/`:** a **primeira** build pode levar vários minutos (Gradle indexa centenas de JARs do IDE). Depois disso, use o **daemon** (comando acima, **sem** `--no-daemon`) — alterações só no Kotlin costumam ficar bem mais rápidas.

Build mínima só para validar compilação:

```bash
./gradlew compileKotlin --console=plain
```

Instale `build/distributions/claude-cli-gui-1.1.0.zip` em **Settings → Plugins → Install from Disk**.

**Dica:** se continuar lento, copie o cache do outro plugin NOW Tech (mesma versão do PhpStorm):

```bash
rm -rf .intellijPlatform
cp -a ../aichat-auto-apply/.intellijPlatform .
```

## Settings

**Settings → Tools → Claude Code** — caminho do CLI, modelo, permissões, aparência.

Para WSL, deixe o executável Linux (`claude` ou caminho completo no WSL).
