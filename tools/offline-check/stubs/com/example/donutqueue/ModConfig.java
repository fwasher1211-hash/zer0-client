package com.example.donutqueue;
public class ModConfig {
  private static final com.google.gson.Gson GSON = null;
  private static final java.nio.file.Path PATH = null;
  public java.util.List<java.lang.String> chatPatterns;
  public java.util.List<java.lang.String> whisperPatterns;
  public java.util.List<java.lang.String> orderPatterns;
  public int hudX;
  public int hudY;
  public boolean hudEnabled;
  public int flicksRequired;
  public boolean restrictLever;
  public int leverX;
  public int leverY;
  public int leverZ;
  public boolean leverPaused;
  public java.util.List<java.lang.String> blacklist;
  public double minimumAmount;
  public double goalAmount;
  public double earnedTotal;
  public boolean trackingEnabled;
  public boolean goalReachedNotified;
  public long celebrationEndTimeMillis;
  public int startBalBoxX;
  public int startBalBoxY;
  public boolean startBalBoxEnabled;
  public transient boolean awaitingBalCapture;
  public double startingBalance;
  public boolean hasStartingBalance;
  public transient boolean awaitingOrderBalCapture;
  public java.util.List<java.lang.String> balPatterns;
  public java.util.List<java.lang.String> earnPatterns;
  public java.util.List<java.lang.String> spendPatterns;
  public int queueBoxColor;
  public int startBalBoxColor;
  public boolean biddingBoxEnabled;
  public int biddingBoxX;
  public int biddingBoxY;
  public int biddingBoxColor;
  public boolean biddingActive;
  public boolean biddingFinished;
  public long biddingEndTimeMillis;
  public long biddingTotalDurationMillis;
  public java.lang.String biddingLeaderName;
  public double biddingLeaderAmount;
  public double biddingMinimum;
  public static final int BID_PRESET_SLOTS = 0;
  public java.util.List<ModConfig.BidPreset> biddingPresets;
  public java.lang.String biddingItemId;
  public int biddingItemCount;
  public java.util.List<java.lang.String> biddingBlacklist;
  public boolean infoBoxEnabled;
  public int infoBoxX;
  public int infoBoxY;
  public int infoBoxColor;
  public float infoBoxScale;
  public float hudScale;
  public float startBalBoxScale;
  public float biddingBoxScale;
  public float earningsBoxScale;
  public java.lang.String infoIgn;
  public java.lang.String infoMin;
  public java.lang.String infoMax;
  public java.lang.String infoGame;
  public java.lang.String infoCustomText;
  public boolean earningsTrackingEnabled;
  public java.util.LinkedHashMap<java.lang.String, java.lang.Double> earningsTotals;
  public int earningsShowCount;
  public boolean earningsTopShowLost;
  public java.util.List<java.lang.String> nonGamblePlayers;
  public boolean rankColorsEnabled;
  public int staffColor;
  public int menuColor;
  public int menuStyle;
  public boolean menuBackdropDim;
  public int menuPanelOpacity;
  public boolean menuAnimEnabled;
  public int menuAnimStyle;
  public int menuAnimSpeed;
  public boolean mainShowStartBal;
  public boolean mainShowToday;
  public boolean mainShowGraph;
  public boolean mainShowStats;
  public boolean mainShowButtons;
  public java.util.List<java.lang.String> mainOrder;
  public boolean mainFourButtons;
  public java.util.List<java.lang.String> mainButtons;
  public int mainGraphRange;
  public boolean mainGraphGains;
  public java.util.List<java.lang.String> menuHiddenTabs;
  public boolean chatPaymentsEnabled;
  public boolean chatWhispersEnabled;
  public int chatNameColor;
  public int chatReceivedColor;
  public int chatSentColor;
  public boolean earningsBoxEnabled;
  public int earningsBoxX;
  public int earningsBoxY;
  public int earningsBoxColor;
  public int liveBoxColor;
  public int liveBoxPos;
  public boolean queueChatMessagesEnabled;
  public boolean startBalChatMessagesEnabled;
  public boolean biddingChatMessagesEnabled;
  public boolean earningsChatMessagesEnabled;
  public ModConfig() {  }
  public void ensureBidPresets() {  }
  public static float clampScale(float a0) { return 0; }
  public static ModConfig load() { return null; }
  private static boolean fixNamePatterns(java.util.List<java.lang.String> a0) { return false; }
  public void save() {  }
  public static class BidPreset {
    public java.lang.String itemId;
    public int count;
    public long seconds;
    public double minimum;
    public BidPreset() {  }
    public boolean isEmpty() { return false; }
  }
}
