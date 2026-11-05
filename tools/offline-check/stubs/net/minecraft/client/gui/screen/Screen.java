package net.minecraft.client.gui.screen;
import net.minecraft.client.gui.*; import net.minecraft.client.input.*;
public class Screen {
  protected net.minecraft.client.MinecraftClient client; public int width, height; protected net.minecraft.client.font.TextRenderer textRenderer;
  protected Screen(net.minecraft.text.Text t){}
  protected void init(){}
  public void render(DrawContext g,int x,int y,float d){}
  public void renderBackground(DrawContext g,int x,int y,float d){}
  public boolean mouseClicked(Click c, boolean dbl){return false;}
  public boolean mouseReleased(Click c){return false;}
  public boolean mouseDragged(Click c, double dx, double dy){return false;}
  public boolean mouseScrolled(double a,double b,double c,double d){return false;}
  public boolean keyPressed(KeyInput k){return false;}
  public boolean charTyped(CharInput c){return false;}
  public void close(){}
  public void removed(){}
  public boolean shouldCloseOnEsc(){return true;}
  public boolean shouldPause(){return true;}
}
