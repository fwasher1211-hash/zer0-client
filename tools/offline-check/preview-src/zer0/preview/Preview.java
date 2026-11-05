package dev.zer0.preview;

import com.example.donutqueue.*;
import dev.zer0.client.config.Zer0Settings;
import dev.zer0.client.spotify.*;
import dev.zer0.client.ui.core.*;
import dev.zer0.client.ui.screens.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

public class Preview {
    static final File OUT = new File("/home/claude/preview/out");

    public static void main(String[] a) throws Exception {
        OUT.mkdirs();
        new File("/home/claude/build-check/cfg/zer0client").mkdirs();
        fakeOriginal();
        fakeSpotify();
        Zer0Settings z = Zer0Settings.get();

        shot(new Zer0TitleScreen(), "01-title-aurora", 140, 150);
        z.layout = "glass"; shot(new HubScreen(null, "home"), "02-hub-glass-home", -1, -1);
        shot(new HubScreen(null, "queue"), "03-hub-glass-queue", -1, -1);
        shot(new HubScreen(null, "appearance"), "04-hub-appearance", -1, -1);
        shot(new HubScreen(null, "layouts"), "05-hub-layouts", -1, -1);
        shot(new HubScreen(null, "backgrounds"), "06-hub-backgrounds", -1, -1);
        shot(new HubScreen(null, "fonts"), "07-hub-fonts", -1, -1);
        shot(new HubScreen(null, "spotify"), "08-hub-spotify", -1, -1);
        z.layout = "dashboard"; z.background = "purple_blue"; shot(new HubScreen(null, "home"), "09-layout-dashboard", -1, -1);
        z.layout = "compact"; z.background = "dark_gradient"; shot(new HubScreen(null, "queue"), "10-layout-compact", -1, -1);
        z.layout = "minimal"; z.background = "landscape"; shot(new HubScreen(null, "modules"), "11-layout-minimal", -1, -1);
        z.layout = "premium"; z.background = "abstract"; shot(new HubScreen(null, "home"), "12-layout-premium", -1, -1);
        z.layout = "glass"; z.dark = false; z.background = "frost"; shot(new HubScreen(null, "appearance"), "13-light-theme", -1, -1);
        z.dark = true; z.background = "black"; z.accent = 0x3DDC97; shot(new Zer0TitleScreen(), "14-title-black-green", -1, -1);
        z.accent = 0x8B7CFF; z.background = "aurora";
        spotifyHuds();
        System.exit(0);
    }

    static void shot(Screen s, String name, float mx, float my) throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        Field cf = Screen.class.getDeclaredField("client"); cf.setAccessible(true); cf.set(s, mc);
        s.width = 640; s.height = 360;
        Method init = s.getClass().getSuperclass().getDeclaredMethod("init");
        Method i2;
        try { i2 = s.getClass().getDeclaredMethod("init"); } catch (NoSuchMethodException e) { i2 = null; }
        Method m = findInit(s.getClass()); m.setAccessible(true); m.invoke(s);
        RasterContext g = null;
        long end = System.currentTimeMillis() + 2600;
        while (System.currentTimeMillis() < end) {
            g = new RasterContext(640, 360);
            s.render(g, Math.round(mx), Math.round(my), 0);
            Thread.sleep(30);
        }
        ImageIO.write(g.img, "png", new File(OUT, name + ".png"));
        System.out.println("wrote " + name);
    }
    static Method findInit(Class<?> c) throws Exception {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) try { return k.getDeclaredMethod("init"); } catch (NoSuchMethodException ignored) {}
        throw new NoSuchMethodException();
    }

    static void fakeOriginal() throws Exception {
        ModConfig c = new ModConfig();
        for (Field f : ModConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            if (f.getType() == List.class) f.set(c, new ArrayList<>());
            else if (f.getType() == String.class) f.set(c, "");
            else if (f.getType() == float.class) f.setFloat(c, 1f);
            else if (f.getType() == LinkedHashMap.class) f.set(c, new LinkedHashMap<>());
        }
        c.mainOrder = new ArrayList<>(List.of("START_BAL", "TODAY", "GRAPH", "STATS", "BUTTONS"));
        c.mainButtons = new ArrayList<>(List.of("SESSION", "STATS", "BIDDING", "TRACKING"));
        c.mainShowStartBal = c.mainShowToday = c.mainShowGraph = c.mainShowStats = c.mainShowButtons = true;
        c.flicksRequired = 2; c.queueBoxColor = 0xFF3399FF; c.earningsShowCount = 5; c.goalAmount = 500_000_000; c.earnedTotal = 186_400_000; c.trackingEnabled = true;
        c.blacklist.addAll(List.of("AltAccount", "Friend123", "Shopkeeper")); c.hudEnabled = true;
        DonutQueueClient.config = c;
        DonutQueueClient.queue = new QueueManager();
    }

    static void fakeSpotify() throws Exception {
        SpotifyClient sc = SpotifyClient.get();
        Field st = SpotifyClient.class.getDeclaredField("state"); st.setAccessible(true); st.set(sc, SpotifyClient.State.CONNECTED);
        Field tr = SpotifyClient.class.getDeclaredField("track"); tr.setAccessible(true);
        tr.set(sc, new SpotifyClient.Track("x", "Midnight City", "M83", "Hurry Up, We're Dreaming", "", 243000, 97000, true, System.currentTimeMillis(), 64, true, "PC"));
        Zer0Settings.get().spotify.clientId = "preview"; Zer0Settings.get().spotify.refreshToken = "preview";
        // a placeholder cover (abstract gradient), handed to the real artwork pipeline
        BufferedImage img = new BufferedImage(640, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0xFF5C8A), 640, 640, new Color(0x3B2BD6)));
        g.fillRect(0, 0, 640, 640);
        g.setColor(new Color(255, 255, 255, 90)); g.fillOval(130, 100, 370, 370);
        g.dispose();
        Field src = SpotifyArt.class.getDeclaredField("src"); src.setAccessible(true); src.set(null, img);
    }

    static void spotifyHuds() throws Exception {
        Zer0Settings.Spotify s = Zer0Settings.get().spotify;
        s.showVolume = true;
        RasterContext g = null;
        long end = System.currentTimeMillis() + 2600;
        while (System.currentTimeMillis() < end) {
            g = new RasterContext(400, 240);
            Anim.frame(); Theme.refresh();
            Backgrounds.previewId = "landscape";
            Backgrounds.draw(g, 0, 0, 400, 240, 1f);
            s.layout = "card"; SpotifyHud.draw(g, 14, 14, 1f, 0.92f, 116, 69);
            s.layout = "compact"; SpotifyHud.draw(g, 14, 104, 1f, 0.92f, -1, -1);
            s.layout = "cover"; SpotifyHud.draw(g, 262, 14, 1f, 0.92f, -1, -1);
            Thread.sleep(30);
        }
        ImageIO.write(g.img, "png", new File(OUT, "15-spotify-hud-layouts.png"));
        Backgrounds.previewId = null;
        System.out.println("wrote spotify");
    }
}
