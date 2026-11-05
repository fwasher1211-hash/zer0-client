package com.example.donutqueue;
public class PresetEditorScreen extends NeonScreen {
  private static final int W = 0;
  private static final int H = 0;
  private static final int COLS = 0;
  private static final int ROWS = 0;
  private static final int CELL = 0;
  private static final int PER_PAGE = 0;
  private static java.util.List<PresetEditorScreen.Entry> allItems;
  private net.minecraft.client.gui.screen.Screen parent;
  private int slot;
  private int left;
  private int top;
  private java.lang.String selectedId;
  private java.lang.String countText;
  private java.lang.String timeText;
  private java.lang.String minText;
  private java.lang.String searchText;
  private java.lang.String status;
  private int page;
  private java.util.List<PresetEditorScreen.Entry> filtered;
  static java.util.List<PresetEditorScreen.Entry> all() { return new java.util.ArrayList<>(); }
  public PresetEditorScreen(net.minecraft.client.gui.screen.Screen a0, int a1) { super(null); }
  protected boolean showPanel() { return false; }
  private void refilter() {  }
  private int pageCount() { return 0; }
  private void setPage(int a0) {  }
  private void pick(int a0) {  }
  private int parseCount() { return 0; }
  private void confirm() {  }
  record Entry(java.lang.String id, java.lang.String name, java.lang.String lower, net.minecraft.item.ItemStack stack) {}
}
