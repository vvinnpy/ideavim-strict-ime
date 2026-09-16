# Vim Strict IME Guard

A small IntelliJ Platform plugin for Linux + Fcitx5 + IdeaVim.

It prevents Chinese and other non-ASCII characters from being committed while IdeaVim is in command modes such as `COMMAND`, `VISUAL`, `SELECT`, or `OP_PENDING`. Insert and Replace modes are left alone, so this plugin can work alongside `IdeaVimExtension`.

## Requirements

- IntelliJ Platform 2026.2 (build 262)
- IdeaVim 2.46.2 or compatible
- Linux
- Fcitx5 with `fcitx5-remote` available on `PATH`

## Behavior

- Uses IdeaVim mode state through a small reflection bridge.
- Intercepts AWT `InputMethodEvent` commits in strict modes.
- Drops non-ASCII typed characters as a fallback.
- Calls `fcitx5-remote -c` with a short cooldown.
- Fails open: if IdeaVim's mode cannot be queried, normal IDEA input is not blocked.

## Install manually

1. Download the plugin ZIP from the latest GitHub Release.
2. Open `Settings -> Plugins`.
3. Use the gear menu and choose `Install Plugin from Disk...`.
4. Select the ZIP and restart the IDE.

## Add as a custom plugin repository

After the repository's GitHub Pages site is enabled, add this URL to
`Settings -> Plugins -> Manage Plugin Repositories...`:

```text
https://<OWNER>.github.io/<REPOSITORY>/updatePlugins.xml
```

GitHub Actions publishes `updatePlugins.xml` from the default branch after a
successful `Release` workflow, so GitHub Pages does not need to allow tag
refs through its environment protection rules.

## Release

Push a tag such as `v0.1.0`. The release workflow builds the plugin ZIP,
uploads it to GitHub Releases, and publishes the custom repository index to
GitHub Pages.

## Notes

This plugin deliberately keeps the scope narrow. It does not own Insert-mode
input-method restoration; `IdeaVimExtension` can continue to do that on Linux.
