package com.example.donutqueue;
final class Sessions {
  private static final int MAX_POINTS = 0;
  private static final long SAVE_EVERY_MS = 0;
  private static final com.google.gson.Gson GSON = null;
  private static final java.util.List<Sessions.Session> sessions = null;
  private static boolean loaded;
  private static boolean dirty;
  private static long lastSave;
  private Sessions() {  }
  private static java.nio.file.Path file() { return null; }
  private static synchronized void ensureLoaded() {  }
  private static void thin(Sessions.Session a0, int a1) {  }
  private static void compactAll() {  }
  static synchronized void saveNow() {  }
  private static void saveSoon() {  }
  static synchronized boolean isRunning() { return false; }
  static synchronized Sessions.Session running() { return null; }
  static synchronized Sessions.Session start(ModConfig a0) { return null; }
  static synchronized Sessions.Session end() { return null; }
  static synchronized void onMoney(double a0) {  }
  static synchronized void onPlayer(java.lang.String a0, double a1) {  }
  private static java.lang.String dateOf(Sessions.Session a0) { return ""; }
  static synchronized java.lang.String date(Sessions.Session a0) { return ""; }
  static synchronized java.util.List<Sessions.Session> allNewestFirst() { return new java.util.ArrayList<>(); }
  static synchronized boolean delete(Sessions.Session a0) { return false; }
  static synchronized java.util.List<Sessions.Session> forDate(java.lang.String a0) { return new java.util.ArrayList<>(); }
  static synchronized double netForDate(java.lang.String a0) { return 0; }
  static synchronized double net(Sessions.Session a0) { return 0; }
  static synchronized boolean hasBalance(Sessions.Session a0) { return false; }
  static synchronized double startBalance(Sessions.Session a0) { return 0; }
  static synchronized double endBalance(Sessions.Session a0) { return 0; }
  static synchronized long startMillis(Sessions.Session a0) { return 0; }
  static synchronized long endMillis(Sessions.Session a0) { return 0; }
  static synchronized long durationSeconds(Sessions.Session a0) { return 0; }
  static synchronized double[][] points(Sessions.Session a0) { return new double[0][]; }
  static synchronized java.util.List<PaymentLog.Payment> payments(Sessions.Session a0) { return new java.util.ArrayList<>(); }
  static synchronized int paymentCount(Sessions.Session a0) { return 0; }
  static synchronized int events(Sessions.Session a0) { return 0; }
  static synchronized java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topGains(Sessions.Session a0, int a1) { return new java.util.ArrayList<>(); }
  static synchronized java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topLosses(Sessions.Session a0, int a1) { return new java.util.ArrayList<>(); }
  static synchronized double totalLoss(Sessions.Session a0) { return 0; }
  static synchronized void clear() {  }
  static class Session {
    long startMillis;
    long endMillis;
    double startBalance;
    boolean hasBalance;
    double net;
    java.util.List<double[]> pts;
    java.util.LinkedHashMap<java.lang.String, java.lang.Double> players;
    java.util.List<PaymentLog.Payment> log;
    Session() {  }
  }
}
