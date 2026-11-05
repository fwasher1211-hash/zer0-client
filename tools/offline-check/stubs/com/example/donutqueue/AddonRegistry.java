package com.example.donutqueue;
final class AddonRegistry {
  private static java.util.List<DonutQueueAddon> cache;
  private AddonRegistry() {  }
  static synchronized java.util.List<DonutQueueAddon> all() { return new java.util.ArrayList<>(); }
}
