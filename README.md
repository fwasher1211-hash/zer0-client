# ZER0 CLIENT

A premium, frosted-glass redesign of **DonutSMP Streamer Utility's** (Fabric, Minecraft **1.21.11**).
Every feature of the original mod is kept and wired into a brand-new interface:

* a new **main menu** that replaces Minecraft's title screen,
* the **ZER0 Hub** with sidebar navigation, search, and 24 pages,
* **5 layouts**, **9 backgrounds**, light and dark themes, an accent colour, transparency and blur controls,
* **15 bundled fonts** with a per-role picker and a live preview,
* a **Spotify HUD** (three layouts) that uses Spotify's official API,
* a drag-and-drop **HUD editor**, and **glass HUD boxes**.

---

## 1. Build it on Windows (about 5 minutes the first time)

1. **Install Java 21 (JDK).** Use Microsoft Build of OpenJDK 21 or Eclipse Temurin 21. Tick *"Set JAVA_HOME"* during setup.
2. Unzip this project somewhere simple, for example `C:\ZER0`.
3. Double-click **`BUILD-ZER0.bat`**. The first build downloads Gradle, Minecraft, Fabric and the fonts.
4. When it finishes, File Explorer opens `build\dist\`. Your mod is **`ZER0-CLIENT-1.0.0.jar`**.

**Installing it**

* Install **Fabric Loader 0.18.4 or newer** for **1.21.11**, plus **Fabric API** for 1.21.11. Mod Menu is optional.
* Put `ZER0-CLIENT-1.0.0.jar` in `.minecraft\mods`.
* **Remove the original `DonutSMP Streamer Utility's` jar.** ZER0 already contains it, and having both would crash the game.
* Your existing streamer settings carry over automatically. ZER0 reads the same `config/donutqueue*.json` files.

If you prefer the command line, run `gradlew build` and look in `build/dist/`.

---

## 2. Status: what has been verified, and what still needs your first build

I don't want to overstate this, so here is exactly where the project stands.

| Check | Result |
|---|---|
| Original jar decompiled at bytecode level: all 61 classes, every config field, every screen, every action | Done. See section 4. |
| All ZER0 Java code compiles (101 classes, `javac -Xlint:all`, zero warnings) | Yes, **against API stubs**. |
| Calls into the original mod checked against its real signatures (stubs generated from the jar's own bytecode) | Yes. |
| Visual output verified: every draw call replayed by a software rasteriser at GUI scale 3 (see `docs/previews/`) | Yes. |
| Compiled against the **real** Minecraft 1.21.11 / Fabric API jars | **Not yet.** The authoring sandbox couldn't reach Fabric's servers, so your first `BUILD-ZER0.bat` run is the real compile. |
| Tested in-game | **Not yet**, for the same reason. |

All Minecraft API calls that I couldn't check against the real game sit in **one file**,
`src/main/java/dev/zer0/client/compat/Mc.java`. Each one is marked `VERIFY`. If Minecraft or Yarn renamed
something, the compiler errors will point to that file, or to the few screen input methods listed below.
**Paste me the error output and I'll fix it.**

### If the build fails

| Error mentions… | Fix |
|---|---|
| `Could not find net.fabricmc:yarn…` / `fabric-api…` | Run `tools\update-versions.bat`, then build again. It fetches the current version numbers from Fabric. |
| `Loom … requires Gradle X` | Put that Gradle version into `gradle/wrapper/gradle-wrapper.properties`. |
| `StyleSpriteSource` (in `Mc.fontStyle`) | Older font API: `Style.EMPTY.withFont(fontId)`. |
| `KeyBinding.Category.create` | Use the name Yarn shows for `KeyBinding.Category`'s factory method. |
| `Click`, `KeyInput`, `CharInput` accessors, or `mouseClicked(Click, boolean)` | These are the 1.21.9+ input records. Use the accessor names your IDE suggests. |
| `drawTexture(... int color)` | Drop the colour argument. Album-art and background fades then become instant. |
| `ConnectScreen.connect` / `PositionedSoundInstance.master` | Match the signature your IDE shows. Both are only used by the *Play* button and the click sound. |
| `ScreenMouseEvents.allowMouseClick` lambda | Fabric API's listener signature. This only affects clicking the Spotify HUD while chat is open. |

---

## 3. Using ZER0

* **Main menu.** This replaces Minecraft's title screen. *Play* joins the server set in General (default `donutsmp.net`). *Singleplayer*, *Multiplayer* and *Settings* open the normal Minecraft screens. *Classic menu* opens the vanilla title screen whenever you want it.
* **Hub.** Your existing *Open Streamer Menu* key (from the original mod) opens it, as do the main menu and Mod Menu. Press **Ctrl+F** to search every setting.
* **HUD editor.** Open it from the HUD page, or from any *Move box* button. Drag a box to move it; it snaps to edges, centre lines and other boxes. Arrow keys nudge a box (hold Shift for 10 px).
* **Layouts.** Switch instantly, with no restart:

  | Layout | What it looks like |
  |---|---|
  | Minimal | Top navigation, no panels, lots of space |
  | Glass | Frosted sidebar and panels with soft shadows |
  | Compact | Icon rail, small rows, two columns |
  | Dashboard | Live statistics tiles and a chart on the home page |
  | Premium | Large type, a hero header and slower, elegant motion |
* **Backgrounds.** Click a tile to preview it behind the hub, then choose **Apply** or **Cancel**.
* **Spotify, one-time setup:**
  1. Go to <https://developer.spotify.com/dashboard>, create an app and tick **Web API**.
  2. Add the Redirect URI **`http://127.0.0.1:8888/callback`**.
  3. Under **User Management**, add the Spotify account you will log in with. Apps in development mode only work for listed users.
  4. In ZER0, go to Spotify, paste the app's **Client ID** and press **Connect**. Your browser opens Spotify's own login page.

  ZER0 uses Authorization Code with PKCE, so **no client secret exists or is stored**. Only a refresh token is saved, locally in `config/zer0client/settings.json`. It is never written into exported profiles, and *Disconnect* deletes it. Showing the current track works on every account; **play/pause/skip/volume need Spotify Premium**, because that is Spotify's rule. You can also bind Spotify play/pause, next and previous under *Controls → ZER0 CLIENT*.

---

## 4. Original features and where they live now

Everything below still runs on the **original, unmodified code**. ZER0 only replaced the interface.
`Zer0Bridge` is the single place where the new UI calls into the original mod.

| Original feature | In ZER0 |
|---|---|
| Payment queue, skip next, clear, remove, blacklist (anti-gamble), min payment, lever flicks, lever pause, restrict lever, chat messages | Hub → **Queue** |
| `/dq` commands, lever/note-block skipping, keybinds | Unchanged (original code) |
| Starting balance tracking, goal, progress, start/reset | Hub → **Starting Bal** |
| Bidding rounds, leader, timer, min bid, ignored bidders | Hub → **Bidding** |
| Item auctions (item picker, amount, duration, min bid), stop, 5 presets, `/bidding <n>` | Hub → **Auctions** |
| Info Display (IGN, min, max, game, custom text) | Hub → **Info Display**, with live preview |
| Earnings tracking, daily totals, per-day graph, top players/losses, non-gambling list | Hub → **Earnings** |
| Sessions (start/end, history, graph, top gains/losses, payments, delete, live box position/colour) | Hub → **Sessions** |
| Payment log with name search and got/paid/total | Hub → **Payments** |
| Chat payment colours, whisper restyle, colours, preview | Hub → **Chat** |
| Rank colours, staff colour | Hub → **Ranks** |
| Five HUD boxes: show/size/colour/position | Each feature's page, **HUD** page and **HUD editor** |
| Main menu dashboard: sections, order, quick-action buttons, graph range | Hub → **Home** (customisable at the bottom) |
| Addon API (`donutqueue` addons from other mods) | Hub → **Addons** (shown when any are installed) |
| Mod Menu config button | Opens the ZER0 Hub |

**Not carried over (and why):**

* **Interactive tutorial.** It walked you through the *old* screens step by step. Every setting now has a description and a tooltip instead.
* **"Menu customisation" options** (old menu colour, style, animations). These only styled the old menus and are replaced by ZER0's Appearance, Layouts and Animations pages. Their saved values are left untouched.
* **"New additions" changelog.** Replaced by *About ZER0*.

---

## 5. Honest technical notes

* **Blur.** Over the game world, ZER0 uses Minecraft's own menu blur (the *Background blur* slider is that real option). On the main menu, backgrounds are rendered pre-softened. Panels are glass made of translucency, a sheen and a hairline edge; there is no per-panel shader blur. That keeps ZER0 compatible with any GPU, Sodium and Iris.
* **Smooth shapes.** Rounded corners are drawn at the screen's physical pixel density with anti-aliased edges, so they stay smooth at every GUI scale.
* **Fonts.** These are real TrueType fonts rendered by Minecraft's TTF engine. Variable fonts render their Regular weight; *Bold* uses Minecraft's bold style. Fonts download during the build (SIL OFL / Apache 2.0). A font that fails to download is simply not offered.
* **Glass HUD boxes.** Two drawing calls inside the original HUD code (background fill and 1 px border) are redirected to rounded glass. This works in the built jar, which is what you play with. In a `runClient` dev environment the boxes keep their original look.
* **Animated backgrounds** are rendered on a background thread at a capped frame rate, with frames cross-faded. The *Performance* page lowers the quality or turns them off.

---

## 6. Project layout

```
BUILD-ZER0.bat                one-click Windows build
build.gradle                  Loom build + font download + final jar merge
fonts.csv                     font download list
libs/original-donutqueue.jar  the original mod (merged into the final jar)
tools/update-versions.*       refresh Fabric/Yarn version numbers
docs/previews/                offline renders of the UI
src/main/java/com/example/donutqueue/Zer0Bridge.java   the bridge into the original mod
src/main/java/dev/zer0/client/
  Zer0Client.java             entry point (keys, HUD, ticking)
  ScreenRouter.java           title screen + original screens -> ZER0 screens
  GlassHud.java               glass restyle of the original HUD boxes
  compat/Mc.java              every version-sensitive Minecraft call (VERIFY list)
  config/Zer0Settings.java    ZER0 settings + profiles
  mixin/                      setScreen routing, HUD restyle
  spotify/                    PKCE client, album art, HUD
  ui/core/                    renderer, animation, theme, layouts, fonts, backgrounds
  ui/widgets/                 button, toggle, slider, text field, dropdown, segmented, colour picker
  ui/page/                    page / section / row / scroll view framework
  ui/pages/                   all 24 hub pages
  ui/screens/                 main menu, hub, HUD editor
```

## 7. Licences

* Original mod: MIT, © Forsurewater. See `LICENSES/ORIGINAL-MIT-NOTICE.txt`.
* Fonts: SIL OFL 1.1 / Apache 2.0. See `LICENSES/FONTS.txt`.
* Spotify is a trademark of Spotify AB. ZER0 is not affiliated with Spotify, Mojang or Microsoft.
