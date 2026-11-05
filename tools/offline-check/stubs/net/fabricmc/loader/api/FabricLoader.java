package net.fabricmc.loader.api;
public interface FabricLoader {
  static FabricLoader getInstance(){ return new FabricLoader(){ public java.nio.file.Path getConfigDir(){return java.nio.file.Path.of("/home/claude/build-check/cfg");} public boolean isModLoaded(String id){return false;} public java.util.Optional<ModContainer> getModContainer(String id){return java.util.Optional.empty();} }; }
  java.nio.file.Path getConfigDir(); boolean isModLoaded(String id); java.util.Optional<ModContainer> getModContainer(String id);
}
