package net.minecraft.client.font;
import java.awt.*; import java.awt.font.FontRenderContext;
public class TextRenderer {
  public static Font font(boolean bold, float px){ return new Font("Inter", bold?Font.BOLD:Font.PLAIN, 10).deriveFont(px); }
  static final FontRenderContext FRC = new FontRenderContext(null, true, true);
  public int getWidth(String s){ return (int)Math.ceil(font(false,10).getStringBounds(s,FRC).getWidth()); }
  public int getWidth(net.minecraft.text.StringVisitable v){ net.minecraft.text.MutableText t=(net.minecraft.text.MutableText)v; return (int)Math.ceil(font(t.style.bold,10).getStringBounds(t.s,FRC).getWidth()); }
}
