package net.minecraft.client;
import net.minecraft.client.gui.screen.Screen;
public class MinecraftClient {
  private static final MinecraftClient I = new MinecraftClient();
  public static MinecraftClient getInstance(){return I;}
  public net.minecraft.client.font.TextRenderer textRenderer = new net.minecraft.client.font.TextRenderer();
  public net.minecraft.client.world.ClientWorld world;
  public net.minecraft.client.network.ClientPlayerEntity player;
  public net.minecraft.client.option.GameOptions options = new net.minecraft.client.option.GameOptions();
  public Keyboard keyboard;
  public Screen currentScreen;
  public static net.minecraft.client.util.Window WINDOW = new net.minecraft.client.util.Window();
  public net.minecraft.client.util.Window getWindow(){return WINDOW;}
  public net.minecraft.resource.ResourceManager getResourceManager(){return id -> java.util.Optional.of(new Object());}
  public static net.minecraft.client.texture.TextureManager TM = new net.minecraft.client.texture.TextureManager(); public net.minecraft.client.texture.TextureManager getTextureManager(){return TM;}
  public net.minecraft.client.sound.SoundManager getSoundManager(){return null;}
  public void execute(Runnable r){r.run();}
  public void setScreen(Screen s){}
  public void scheduleStop(){}
}
