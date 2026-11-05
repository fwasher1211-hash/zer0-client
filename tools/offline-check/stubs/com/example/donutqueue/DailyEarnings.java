package com.example.donutqueue;
final class DailyEarnings {
  private static final java.util.TreeMap<java.lang.String, double[]> days = null;
  private static final java.util.TreeMap<java.lang.String, java.util.List<double[]>> intraday = null;
  private static final int MAX_POINTS_PER_DAY = 0;
  private static final java.util.TreeMap<java.lang.String, java.util.LinkedHashMap<java.lang.String, java.lang.Double>> byPlayer = null;
  private static boolean loaded;
  private static final java.util.ArrayDeque<java.lang.Double> recent = null;
  private static final int RECENT_MAX = 0;
  private static final java.util.List<java.lang.Double> run = null;
  private static final int RUN_MAX = 0;
  private static boolean dirty;
  private static long lastSave;
  private static int rev;
  private static DailyEarnings.Day[] allCache;
  private static int allCacheRev;
  private static java.lang.String allCacheDay;
  private DailyEarnings() {  }
  private static void pushRecent(double a0) {  }
  private static void pushRun(double a0) {  }
  static synchronized double[] runSeries() { return new double[0]; }
  static synchronized void clearRun() {  }
  static synchronized double[] recentBalances() { return new double[0]; }
  private static java.nio.file.Path file() { return null; }
  private static java.lang.String todayKey() { return ""; }
  private static synchronized void ensureLoaded() {  }
  private static void trim() {  }
  private static void saveSoon() {  }
  static synchronized int revision() { return 0; }
  static synchronized void save() {  }
  private static double[] today() { return new double[0]; }
  static synchronized void record(double a0, ModConfig a1) {  }
  static synchronized void recordPlayer(java.lang.String a0, double a1) {  }
  static synchronized java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topPlayers(java.lang.String a0, int a1) { return new java.util.ArrayList<>(); }
  static boolean isNonGamble(java.lang.String a0) { return false; }
  static synchronized void markNonGamble(java.lang.String a0) {  }
  static synchronized boolean unmarkNonGamble(java.lang.String a0) { return false; }
  static synchronized void clearNonGamble() {  }
  static synchronized java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topLosses(java.lang.String a0, int a1) { return new java.util.ArrayList<>(); }
  static synchronized java.util.List<java.util.Map.Entry<java.lang.String, java.lang.Double>> topLossesOver(java.util.List<java.lang.String> a0, int a1) { return new java.util.ArrayList<>(); }
  static synchronized double totalLossOver(java.util.List<java.lang.String> a0) { return 0; }
  private static void addPoint(java.lang.String a0, int a1, double a2) {  }
  static synchronized double[][] dayPoints(java.lang.String a0) { return new double[0][]; }
  static synchronized void markStart(ModConfig a0) {  }
  static synchronized double todayNet() { return 0; }
  static synchronized DailyEarnings.Day[] allDays() { return new DailyEarnings.Day[0]; }
  static synchronized boolean hasData() { return false; }
  static synchronized void clear() {  }
  static class Day {
    java.lang.String date;
    double net;
    double balance;
    Day(java.lang.String a0, double a1, double a2) {  }
  }
}
