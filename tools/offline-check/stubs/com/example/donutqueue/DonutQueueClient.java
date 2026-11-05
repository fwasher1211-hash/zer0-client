package com.example.donutqueue;
public class DonutQueueClient implements net.fabricmc.api.ClientModInitializer {
  public static ModConfig config;
  public static QueueManager queue;
  private java.util.List<java.util.regex.Pattern> compiledPatterns;
  private java.util.List<java.util.regex.Pattern> compiledEarnPatterns;
  private java.util.List<java.util.regex.Pattern> compiledSpendPatterns;
  private java.util.List<java.util.regex.Pattern> compiledBalPatterns;
  private java.util.List<java.util.regex.Pattern> compiledWhisperPatterns;
  private java.util.List<java.util.regex.Pattern> compiledOrderPatterns;
  private int flickCount;
  private static boolean debugChat;
  private static boolean processingChatMessage;
  private static DonutQueueClient instance;
  private static final java.util.regex.Pattern OTHER_BAL_PATTERN = null;
  private static final int EARNINGS_BOX_WIDTH = 0;
  private static final int EARNINGS_BOX_HEIGHT = 0;
  private static final java.net.http.HttpClient SKIN_HTTP_CLIENT = null;
  private static final java.util.Set<java.lang.String> skinLookupInFlight = null;
  private static final java.util.Map<java.lang.String, java.lang.Long> skinLookupFailedAt = null;
  private static final long SKIN_RETRY_COOLDOWN_MILLIS = 0;
  static final int QUEUE_BOX_HEIGHT = 0;
  static final int STARTBAL_BOX_WIDTH = 0;
  static final int STARTBAL_BOX_HEIGHT = 0;
  private static final int STARTBAL_TEXT_WIDTH = 0;
  private static final int STARTBAL_RING_RADIUS = 0;
  public DonutQueueClient() {  }
  static void drawHead(net.minecraft.client.gui.DrawContext a0, java.lang.String a1, int a2, int a3, int a4) {  }
  public void onInitializeClient() {  }
  private void compilePatterns() {  }
  private void registerChatListener() {  }
  private void handleChatMessage(java.lang.String a0) {  }
  private java.lang.String admit(java.lang.String a0, java.lang.String a1) { return ""; }
  static java.lang.String simulate(java.lang.String a0) { return ""; }
  private java.lang.String simulateInternal(java.lang.String a0) { return ""; }
  static java.lang.String keyLabel(java.lang.String a0) { return ""; }
  private static boolean isOtherBalLine(java.lang.String a0) { return false; }
  private boolean isBalLine(java.lang.String a0) { return false; }
  private boolean isWhisperLine(java.lang.String a0) { return false; }
  private void handleBalCapture(java.lang.String a0) {  }
  private void handleOrderPrompt(java.lang.String a0) {  }
  private boolean handleOrderBalReply(java.lang.String a0) { return false; }
  private void handleEarnSpend(java.lang.String a0) {  }
  private static java.lang.String playerName(java.util.regex.Matcher a0, java.lang.String a1) { return ""; }
  private static java.lang.String tryPlayer(java.util.regex.Matcher a0, java.lang.String a1) { return ""; }
  private static java.lang.String tryGroup(java.util.regex.Matcher a0, java.lang.String a1) { return ""; }
  private void adjustEarned(double a0) {  }
  private boolean isBlacklisted(java.lang.String a0) { return false; }
  private void handleEarningsTracking(java.lang.String a0) {  }
  private void addEarnings(java.lang.String a0, double a1) {  }
  private static java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topEarners(int a0) { return new java.util.ArrayList<>(); }
  static int earningsBoxHeight(int a0) { return 0; }
  private static java.lang.String formatMoneySigned(double a0, boolean a1) { return ""; }
  private void renderEarningsBox(net.minecraft.client.gui.DrawContext a0) {  }
  private void drawBalanceGraph(net.minecraft.client.gui.DrawContext a0, int a1, int a2, int a3, int a4, java.lang.String a5, int a6, int a7, double a8, boolean a9) {  }
  private void handleBiddingPayment(java.lang.String a0, java.lang.String a1) {  }
  private void renderBiddingBox(net.minecraft.client.gui.DrawContext a0) {  }
  private void renderInfoBox(net.minecraft.client.gui.DrawContext a0) {  }
  static long parseDurationSeconds(java.lang.String a0) { return 0; }
  static double parseAmount(java.lang.String a0) { return 0; }
  static java.lang.String formatAmount(double a0) { return ""; }
  private void renderPlayerHead(net.minecraft.client.gui.DrawContext a0, java.lang.String a1, int a2, int a3, int a4) {  }
  private void drawPlaceholderHead(net.minecraft.client.gui.DrawContext a0, int a1, int a2, int a3) {  }
  private void requestSkinAsync(java.lang.String a0, java.lang.String a1) {  }
  static boolean isBedrockName(java.lang.String a0) { return false; }
  private static java.lang.String lookupJavaSkinUrl(java.lang.String a0) throws java.lang.Exception { return ""; }
  private static java.lang.String lookupBedrockSkinUrl(java.lang.String a0) throws java.lang.Exception { return ""; }
  private void markFailed(java.lang.String a0) {  }
  private void renderCountdownRing(net.minecraft.client.gui.DrawContext a0, int a1, int a2, int a3, double a4, int a5) {  }
  private void registerHud() {  }
  private void renderLiveSession(net.minecraft.client.gui.DrawContext a0) {  }
  private static int scaleRgb(int a0, float a1) { return 0; }
  private static int mixWhite(int a0, float a1) { return 0; }
  private static void roundedFill(net.minecraft.client.gui.DrawContext a0, int a1, int a2, int a3, int a4, int a5, int a6) {  }
  private static void scaled(net.minecraft.client.gui.DrawContext a0, int a1, int a2, float a3, java.lang.Runnable a4) {  }
  static int scaledSize(int a0, float a1) { return 0; }
  private void renderHud(net.minecraft.client.gui.DrawContext a0) {  }
  private void renderStartBalBox(net.minecraft.client.gui.DrawContext a0) {  }
  private void renderProgressRing(net.minecraft.client.gui.DrawContext a0, int a1, int a2, int a3, double a4, int a5, int a6) {  }
  private void checkGoalReached(double a0) {  }
  private void renderCelebration(net.minecraft.client.gui.DrawContext a0) {  }
  private void drawScaledCentered(net.minecraft.client.gui.DrawContext a0, java.lang.String a1, int a2, int a3, float a4, int a5) {  }
  static java.lang.String formatCompact(double a0) { return ""; }
  private void registerLever() {  }
  private void registerKeybindTick() {  }
  private void registerFlick() {  }
  public static void advanceQueue() {  }
  private void registerCommands() {  }
  private static boolean isBlacklistedStatic(java.lang.String a0) { return false; }
  private void flickCountResetHack() {  }
}
