# Chunky2

Paper/Spigot 1.12.2 plugin that keeps chunks around every tamed wolf loaded.

## Behavior

- Detects every loaded tamed wolf in every world.
- Keeps the wolf's chunk loaded even if its owner is offline.
- Default radius is 1 chunk, so each tamed wolf keeps a 3x3 chunk area active and can cross chunk borders naturally.
- Cancels unloads for tracked wolf chunks.
- Remembers tracked chunks in `config.yml` so they can be restored after a server restart.
- Automatically updates when wolves are tamed, spawned/bred, die, or move during periodic scans.

## Commands

- `/chunkywolves status`
- `/chunkywolves reload`

Permission: `chunky2.admin` (OP by default)

## Config

```yml
chunk-radius: 1
scan-interval-ticks: 40
save-interval-ticks: 600
```

`chunk-radius: 0` keeps only each wolf's current chunk loaded. `1` is recommended.

Note: when installing the plugin for the first time, an existing tamed wolf in a completely unloaded chunk cannot be discovered until that chunk is loaded once. After Chunky2 sees the wolf, it remembers and keeps its chunks loaded.
