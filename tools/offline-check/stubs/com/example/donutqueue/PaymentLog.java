package com.example.donutqueue;
final class PaymentLog {
  private static final int MAX = 0;
  private static final com.google.gson.Gson GSON = null;
  private static final java.util.List<PaymentLog.Payment> list = null;
  private static boolean loaded;
  private static boolean dirty;
  private static long lastSave;
  private static int rev;
  private PaymentLog() {  }
  private static java.nio.file.Path file() { return null; }
  private static synchronized void ensureLoaded() {  }
  static synchronized void saveNow() {  }
  static synchronized void add(java.lang.String a0, double a1) {  }
  static synchronized java.util.List<PaymentLog.Payment> newestFirst() { return new java.util.ArrayList<>(); }
  static synchronized int revision() { return 0; }
  static synchronized void clear() {  }
  static class Payment {
    long t;
    java.lang.String p;
    double a;
    Payment() {  }
    Payment(long a0, java.lang.String a1, double a2) {  }
  }
}
