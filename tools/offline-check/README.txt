Offline verification used while writing ZER0 (no Minecraft download needed):

  stubs/        minimal Minecraft/Fabric API stand-ins + stubs of the original mod generated
                from its bytecode by gen.py (so every call into the original is signature-checked)
  preview-src/  a software DrawContext that replays ZER0's exact draw calls into PNGs
                (docs/previews/*.png were produced with it; text uses the system Inter font)

These are NOT part of the mod and are not needed to build it. Real builds use Gradle + Loom.
