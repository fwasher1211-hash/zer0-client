package net.minecraft.client.gui;
import net.minecraft.client.font.TextRenderer; import net.minecraft.text.Text; import net.minecraft.util.Identifier;
public class DrawContext {
  protected final org.joml.Matrix3x2fStack m = new org.joml.Matrix3x2fStack();
  public void fill(int a,int b,int c,int d,int col){}
  public void fillGradient(int a,int b,int c,int d,int c1,int c2){}
  public void drawText(TextRenderer t, Text s, int x, int y, int c, boolean sh){}
  public void drawText(TextRenderer t, String s, int x, int y, int c, boolean sh){}
  public void drawTexture(com.mojang.blaze3d.pipeline.RenderPipeline p, Identifier id, int x,int y,float u,float v,int w,int h,int rw,int rh,int tw,int th,int color){}
  public void drawTexture(com.mojang.blaze3d.pipeline.RenderPipeline p, Identifier id, int x,int y,float u,float v,int w,int h,int rw,int rh,int tw,int th){}
  public void drawItem(net.minecraft.item.ItemStack s,int x,int y){}
  public org.joml.Matrix3x2fStack getMatrices(){return m;}
  public void enableScissor(int a,int b,int c,int d){}
  public void disableScissor(){}
}
