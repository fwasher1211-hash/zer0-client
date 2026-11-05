package dev.zer0.client.ui.page;

import dev.zer0.client.ui.widgets.Button;
import dev.zer0.client.ui.widgets.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** A titled group of rows, drawn as one rounded card (macOS-settings style). */
public final class Section {
    public final String title;
    public final List<Row> rows = new ArrayList<>();
    public String footer;
    public BooleanSupplier visible = () -> true;
    public boolean bare;   // no card behind it (hero/stat grids)

    public Section(String title) { this.title = title; }

    public Section add(Row r) { rows.add(r); return this; }
    public Section setting(String title, String desc, Widget control) { return add(new Row.Setting(title, desc, control)); }
    public Section setting(String title, String desc, Widget control, Runnable reset, BooleanSupplier changed) {
        return add(new Row.Setting(title, desc, control).reset(reset, changed));
    }
    public Section wide(String title, String desc, Widget control) { return add(new Row.Wide(title, desc, control)); }
    public Section buttons(Button... b) { return add(new Row.Buttons(b)); }
    public Section note(Supplier<String> text) { return add(new Row.Note(text)); }
    public Section note(String text) { return note(() -> text); }
    public Section footer(String f) { this.footer = f; return this; }
    public Section when(BooleanSupplier v) { this.visible = v; return this; }
    public Section bare() { this.bare = true; return this; }
}
