package dev.zer0.client.ui.pages;

import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.ui.page.Page;

import java.util.ArrayList;
import java.util.List;

/** Registry of hub pages, in sidebar order. Pages are created once and reused (state such as scroll survives). */
public final class Pages {
    private Pages() {}
    private static List<Page> all;

    public static List<Page> all() {
        if (all == null) {
            List<Page> l = new ArrayList<>();
            l.add(new HomePages.Home());
            l.add(new HomePages.Modules());
            l.add(new StreamerPages.Queue());
            l.add(new StreamerPages.StartBal());
            l.add(new StreamerPages.Bidding());
            l.add(new DataPages.Auctions());
            l.add(new StreamerPages.Info());
            l.add(new DataPages.Earnings());
            l.add(new DataPages.SessionsPage());
            l.add(new DataPages.Payments());
            l.add(new StreamerPages.Chat());
            l.add(new StreamerPages.Ranks());
            l.add(new StreamerPages.Addons());
            l.add(new ClientPages.General());
            l.add(new ClientPages.Appearance());
            l.add(new ClientPages.Layouts());
            l.add(new ClientPages.BackgroundsPage());
            l.add(new ClientPages.FontsPage());
            l.add(new ClientPages.Hud());
            l.add(new SpotifyPage());
            l.add(new ClientPages.Animations());
            l.add(new ClientPages.Performance());
            l.add(new ClientPages.Configs());
            l.add(new ClientPages.About());
            all = l;
        }
        return all;
    }

    /** Hidden in Settings > General > Sidebar. */
    public static boolean hidden(String id) { return Zer0Settings.get().hiddenPages.contains(id); }

    public static String titleOf(String id) {
        for (Page p : all()) if (p.id.equals(id)) return p.title;
        return id;
    }
    /** Layout / profile changes alter row heights and structure: rebuild everything. */
    public static void rebuildAll() { for (Page p : all()) p.rebuild(); }
}
