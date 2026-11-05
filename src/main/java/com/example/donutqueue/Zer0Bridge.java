package com.example.donutqueue;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ZER0's only door into the original mod.
 *
 * It sits in the original's package so it can reach the package-private classes (Sessions, DailyEarnings,
 * Auction, PaymentLog ...). ZER0's UI never re-implements the original's logic: every action below either
 * calls the original code directly, or - where the original performed the action inline inside one of its
 * old screens - repeats exactly the field writes that were recovered from that screen's bytecode
 * (noted per method).
 */
public final class Zer0Bridge {
    private Zer0Bridge() {}

    // ================================================================ config / queue
    public static ModConfig cfg() { return DonutQueueClient.config; }
    public static void save() { DonutQueueClient.config.save(); }
    public static ModConfig defaults() { return new ModConfig(); }
    public static float clampScale(float f) { return ModConfig.clampScale(f); }

    public record QueueRow(String player, String amount, long time) {}
    private static List<QueueRow> rows(List<QueueEntry> in) {
        List<QueueRow> out = new ArrayList<>();
        for (QueueEntry e : in) out.add(new QueueRow(e.playerName, e.amount, e.timestamp));
        return out;
    }
    public static List<QueueRow> queue() { return rows(DonutQueueClient.queue.asList()); }
    public static List<QueueRow> history(int n) { return rows(DonutQueueClient.queue.getHistory(n)); }
    public static int queueSize() { return DonutQueueClient.queue.size(); }
    public static void advanceQueue() { DonutQueueClient.advanceQueue(); }
    public static void clearQueue() { DonutQueueClient.queue.clear(); }
    public static boolean removeFromQueue(String name) { return DonutQueueClient.queue.removeByName(name); }
    /** Same as the original "Anti Gamble Skip" Add button: blacklist + purge from the queue. */
    public static void blacklistAdd(String name) {
        if (name == null || name.isBlank()) return;
        String n = name.trim();
        if (cfg().blacklist.stream().noneMatch(s -> s.equalsIgnoreCase(n))) cfg().blacklist.add(n);
        DonutQueueClient.queue.removeAllByName(n);
        save();
    }

    // ================================================================ formatting / parsing (original helpers)
    public static String fmt(double v) { return DonutQueueClient.formatAmount(v); }
    public static String fmtCompact(double v) { return DonutQueueClient.formatCompact(v); }
    public static double parseAmount(String s) { return DonutQueueClient.parseAmount(s); }
    public static long parseDurationSeconds(String s) { return DonutQueueClient.parseDurationSeconds(s); }
    public static String keyLabel(String name) { return DonutQueueClient.keyLabel(name); }
    public static String formatDuration(long seconds) { return Auction.formatDuration(seconds); }
    public static void drawHead(DrawContext g, String name, int x, int y, int size) { DonutQueueClient.drawHead(g, name, x, y, size); }

    /** HUD box size in GUI px, using the original's own sizing (same numbers as its "Move box" screens). */
    public static int[] boxSize(String id) {
        ModConfig c = cfg();
        return switch (id) {
            case "queue" -> new int[]{DonutQueueClient.scaledSize(190, c.hudScale), DonutQueueClient.scaledSize(60, c.hudScale)};
            case "startbal" -> new int[]{DonutQueueClient.scaledSize(250, c.startBalBoxScale), DonutQueueClient.scaledSize(54, c.startBalBoxScale)};
            case "bidding" -> new int[]{DonutQueueClient.scaledSize(270, c.biddingBoxScale), DonutQueueClient.scaledSize(70, c.biddingBoxScale)};
            case "info" -> new int[]{DonutQueueClient.scaledSize(190, c.infoBoxScale), DonutQueueClient.scaledSize(65, c.infoBoxScale)};
            case "earnings" -> new int[]{DonutQueueClient.scaledSize(220, c.earningsBoxScale),
                    DonutQueueClient.scaledSize(DonutQueueClient.earningsBoxHeight(c.earningsShowCount), c.earningsBoxScale)};
            default -> new int[]{100, 40};
        };
    }

    // ================================================================ sessions
    public record SessionRow(Object handle, String date, long startMs, long endMs, long durSec, double net,
                             boolean hasBal, double startBal, double endBal, int payments, int events,
                             double loss, boolean running) {}
    public record PaymentRow(long time, String player, double amount) {}
    public record DayRow(String date, double net, double balance) {}
    public record ItemRow(String id, String name, String lower, ItemStack stack) {}

    public static boolean sessionRunning() { return Sessions.isRunning(); }
    /** Original START/END SESSION button (FuturisticMenuScreen.toggleSession), same status text. */
    public static String toggleSession() {
        if (Sessions.isRunning()) { Sessions.end(); return "Session ended."; }
        Sessions.Session s = Sessions.start(cfg());
        return Sessions.hasBalance(s) ? "Session started." : "Session started - start Starting Bal tracking to graph it.";
    }
    public static SessionRow runningSession() {
        Sessions.Session r = Sessions.running();
        return r == null ? null : row(r, r);
    }
    private static SessionRow row(Sessions.Session s, Sessions.Session running) {
        return new SessionRow(s, Sessions.date(s), Sessions.startMillis(s), Sessions.endMillis(s),
                Sessions.durationSeconds(s), Sessions.net(s), Sessions.hasBalance(s), Sessions.startBalance(s),
                Sessions.endBalance(s), Sessions.paymentCount(s), Sessions.events(s), Sessions.totalLoss(s), s == running);
    }
    public static List<SessionRow> sessions() {
        Sessions.Session run = Sessions.running();
        List<SessionRow> out = new ArrayList<>();
        for (Sessions.Session s : Sessions.allNewestFirst()) out.add(row(s, run));
        return out;
    }
    public static boolean deleteSession(SessionRow r) { return Sessions.delete((Sessions.Session) r.handle()); }
    public static double[][] sessionPoints(SessionRow r) { return Sessions.points((Sessions.Session) r.handle()); }
    public static List<Map.Entry<String, Double>> sessionTopGains(SessionRow r, int n) { return Sessions.topGains((Sessions.Session) r.handle(), n); }
    public static List<Map.Entry<String, Double>> sessionTopLosses(SessionRow r, int n) { return Sessions.topLosses((Sessions.Session) r.handle(), n); }
    public static List<PaymentRow> sessionPayments(SessionRow r) {
        List<PaymentRow> out = new ArrayList<>();
        for (PaymentLog.Payment p : Sessions.payments((Sessions.Session) r.handle())) out.add(new PaymentRow(p.t, p.p, p.a));
        return out;
    }
    public static List<PaymentRow> allPayments() {
        List<PaymentRow> out = new ArrayList<>();
        for (PaymentLog.Payment p : PaymentLog.newestFirst()) out.add(new PaymentRow(p.t, p.p, p.a));
        return out;
    }
    public static int paymentRevision() { return PaymentLog.revision(); }

    // ================================================================ daily earnings
    public static double todayNet() { return DailyEarnings.todayNet(); }
    public static boolean hasEarningsData() { return DailyEarnings.hasData(); }
    public static List<DayRow> days() {
        List<DayRow> out = new ArrayList<>();
        for (DailyEarnings.Day d : DailyEarnings.allDays()) out.add(new DayRow(d.date, d.net, d.balance));
        return out;
    }
    public static double[][] dayPoints(String date) { return DailyEarnings.dayPoints(date); }
    public static double[] runSeries() { return DailyEarnings.runSeries(); }
    public static double[] recentBalances() { return DailyEarnings.recentBalances(); }
    public static List<Map.Entry<String, Double>> topPlayers(String date, int n) { return DailyEarnings.topPlayers(date, n); }
    public static List<Map.Entry<String, Double>> topLosses(String date, int n) { return DailyEarnings.topLosses(date, n); }
    public static void markNonGamble(String n) { DailyEarnings.markNonGamble(n); }
    public static boolean unmarkNonGamble(String n) { return DailyEarnings.unmarkNonGamble(n); }
    public static void clearNonGamble() { DailyEarnings.clearNonGamble(); }

    // ================================================================ starting bal / tracking
    /** Original "START TRACKING" (FuturisticMenuScreen.startTracking), field-for-field. */
    public static String startTracking() {
        ModConfig c = cfg();
        c.earnedTotal = 0; c.goalReachedNotified = false; c.trackingEnabled = false;
        c.hasStartingBalance = false; c.awaitingBalCapture = true;
        c.save(); DailyEarnings.clearRun();
        return "Now run /bal in chat - it will start from 0.";
    }
    /** Original "RESET TRACKING" (FuturisticMenuScreen.resetTracking), field-for-field. */
    public static String resetTracking() {
        ModConfig c = cfg();
        c.earnedTotal = 0; c.goalReachedNotified = false; c.trackingEnabled = false;
        c.hasStartingBalance = false; c.awaitingBalCapture = false;
        c.save(); DailyEarnings.clearRun();
        return "Tracking reset.";
    }
    /** Status read-out equivalent to the original "Check Now" button (it only reported, never changed state). */
    public static String trackingStatus() {
        ModConfig c = cfg();
        if (c.awaitingBalCapture) return "Waiting for your /bal ...";
        if (c.trackingEnabled) {
            String s = "Earned " + fmtCompact(c.earnedTotal);
            if (c.goalAmount > 0) s += " of " + fmtCompact(c.goalAmount);
            if (c.hasStartingBalance) s += "  -  started at " + fmtCompact(c.startingBalance);
            return s;
        }
        if (c.goalAmount > 0) return "Goal " + fmtCompact(c.goalAmount) + " set - start tracking to count towards it.";
        return "No goal set and not tracking.";
    }

    // ================================================================ bidding / auctions
    /** Original STOP BIDDING (FuturisticMenuScreen bidding page lambda). */
    public static String stopBidding() {
        ModConfig c = cfg();
        c.biddingActive = false; c.biddingFinished = false; c.biddingLeaderName = ""; c.biddingLeaderAmount = 0;
        c.save();
        return "Bidding stopped and reset.";
    }
    /** Original START BIDDING from the main menu (FuturisticMenuScreen.toggleBidding): 60 s round, no item. */
    public static String startPlainBidding() {
        ModConfig c = cfg();
        c.biddingActive = true; c.biddingFinished = false;
        c.biddingTotalDurationMillis = 60000L; c.biddingEndTimeMillis = System.currentTimeMillis() + 60000L;
        c.biddingLeaderName = ""; c.biddingLeaderAmount = 0; c.biddingBoxEnabled = true;
        c.biddingItemId = ""; c.biddingItemCount = 0;
        c.save();
        return "Bidding started (60s).";
    }
    /** STOP on the original auction screen (BiddingMenuScreen) called Auction.stop(). */
    public static void auctionStop() { Auction.stop(); }
    public static void startAuction(String itemId, int count, long seconds, double minimum) { Auction.start(itemId, count, seconds, minimum); }
    public static boolean startPreset(int slot) { return Auction.startPreset(slot); }
    public static String itemName(String id) { return Auction.itemName(id); }
    public static ItemStack stack(String id) { return Auction.stackFor(id); }
    public static int presetSlots() { return ModConfig.BID_PRESET_SLOTS; }
    public static List<ModConfig.BidPreset> presets() { cfg().ensureBidPresets(); return cfg().biddingPresets; }
    public static void savePreset(int slot, String itemId, int count, long seconds, double min) {
        ModConfig.BidPreset p = new ModConfig.BidPreset();
        p.itemId = itemId; p.count = count; p.seconds = seconds; p.minimum = min;
        presets().set(slot, p);
        save();
    }
    public static void clearPreset(int slot) { presets().set(slot, new ModConfig.BidPreset()); save(); }

    private static List<ItemRow> items;
    /** Every item, as the original preset editor lists them. */
    public static synchronized List<ItemRow> items() {
        if (items == null) {
            List<ItemRow> list = new ArrayList<>();
            for (PresetEditorScreen.Entry e : PresetEditorScreen.all()) list.add(new ItemRow(e.id(), e.name(), e.lower(), e.stack()));
            items = list;
        }
        return items;
    }

    // ================================================================ chat styling previews (original ChatStyle)
    public static Text previewPaid(Text t) { return ChatStyle.recolourPayment(t); }
    public static Text previewWhisperOut(String who, String msg) { return ChatStyle.whisperOut(who, msg); }
    public static Text previewWhisperIn(String who, String msg) { return ChatStyle.whisperIn(who, msg); }

    // ================================================================ addons (other mods using the original's API)
    public record AddonRow(String label, Object handle) {}
    public static List<AddonRow> addons() {
        List<AddonRow> out = new ArrayList<>();
        try { for (DonutQueueAddon a : AddonRegistry.all()) out.add(new AddonRow(a.menuLabel(), a)); }
        catch (Throwable t) { System.err.println("[ZER0] addon scan failed: " + t); }
        return out;
    }
    public static Screen openAddon(AddonRow a, Screen parent) {
        try { return ((DonutQueueAddon) a.handle()).createScreen(parent); } catch (Throwable t) { return null; }
    }

    /** True for screens that belong to the original mod (they get replaced by ZER0 pages). */
    public static boolean isOriginalScreen(Screen s) {
        return s != null && s.getClass().getName().startsWith("com.example.donutqueue.") && !(s instanceof Zer0Marker);
    }
    /** Marker so ZER0's own classes placed in this package are never routed. */
    public interface Zer0Marker {}
}
