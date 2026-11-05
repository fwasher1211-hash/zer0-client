package com.example.donutqueue;
public class QueueManager {
  private static final com.google.gson.Gson GSON = null;
  private static final java.nio.file.Path PATH = null;
  private static final java.nio.file.Path HISTORY_PATH = null;
  private static final int HISTORY_MAX = 0;
  private java.util.Deque<QueueEntry> queue;
  private java.util.Deque<QueueEntry> history;
  public QueueManager() {  }
  public synchronized void add(QueueEntry a0) {  }
  public synchronized QueueEntry pollNext() { return null; }
  public synchronized void recordServed(QueueEntry a0) {  }
  public synchronized java.util.List<QueueEntry> getHistory(int a0) { return new java.util.ArrayList<>(); }
  public synchronized QueueEntry peekNext() { return null; }
  public synchronized java.util.List<QueueEntry> asList() { return new java.util.ArrayList<>(); }
  public synchronized int size() { return 0; }
  public synchronized void clear() {  }
  public synchronized boolean removeByName(java.lang.String a0) { return false; }
  public synchronized int removeAllByName(java.lang.String a0) { return 0; }
  private void save() {  }
  private void load() {  }
  private void saveHistory() {  }
  private void loadHistory() {  }
}
