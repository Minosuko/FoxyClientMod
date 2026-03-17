package com.foxyclient.gui;

import com.foxyclient.setting.BlockListSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A searchable block-grid picker screen.
 * Click a block to add/remove it from the linked {@link BlockListSetting}.
 */
public class BlockSelectorScreen extends Screen {

    // ── Theme (matches ClickGUI) ────────────────────────────────────────
    private static final int COL_BG        = 0xF0101520;
    private static final int COL_HEADER    = 0xFF0D1117;
    private static final int COL_BORDER    = 0xFF30363D;
    private static final int COL_ACCENT    = 0xFF00E5CC;
    private static final int COL_SELECTED  = 0x6000E5CC;
    private static final int COL_HOVER     = 0x30FFFFFF;
    private static final int COL_TEXT      = 0xFFE6EDF3;
    private static final int COL_TEXT_DIM  = 0xFF8B949E;
    private static final int COL_SEARCH_BG = 0xFF161B22;
    private static final int COL_SLOT_BG   = 0xFF1C2333;

    // ── Layout ──────────────────────────────────────────────────────────
    private static final int CELL   = 20;  // cell size (icon 16 + 2px padding each side)
    private static final int COLS   = 14;
    private static final int SEARCH_H = 22;
    private static final int MARGIN = 8;

    // ── State ───────────────────────────────────────────────────────────
    private final BlockListSetting setting;
    private final Runnable onChanged;
    private final Screen parent;

    private String searchQuery = "";
    private final List<Block> allBlocks = new ArrayList<>();
    private final List<Block> filteredBlocks = new ArrayList<>();
    private int scrollOffset = 0;

    // Panel geometry (computed in init)
    private int panelX, panelY, panelW, panelH;
    private int gridY;    // top of the grid area
    private int gridH;    // height of the grid area
    private int visibleRows;

    public BlockSelectorScreen(BlockListSetting setting, Runnable onChanged, Screen parent) {
        super(Text.literal("Block Selector"));
        this.setting = setting;
        this.onChanged = onChanged;
        this.parent = parent;

        // Collect all blocks that have an item form (skip air and technical blocks)
        for (Block block : Registries.BLOCK) {
            if (block == Blocks.AIR) continue;
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                allBlocks.add(block);
            }
        }
        rebuildFilter();
    }

    @Override
    protected void init() {
        super.init();
        panelW = COLS * CELL + MARGIN * 2;
        panelH = Math.min(height - 40, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        gridY  = panelY + SEARCH_H + MARGIN + 14;  // below search + header label
        gridH  = panelH - (gridY - panelY) - MARGIN;
        visibleRows = gridH / CELL;
    }

    // ── Rendering ───────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Dim overlay
        ctx.fill(0, 0, width, height, 0x88000000);

        // Panel background
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COL_BG);
        // Top accent bar
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 2, COL_ACCENT);
        // Border
        ctx.fill(panelX, panelY + panelH, panelX + panelW, panelY + panelH + 1, COL_BORDER);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, COL_BORDER);
        ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, COL_BORDER);

        var tr = client.textRenderer;

        // Search bar
        int searchX = panelX + MARGIN;
        int searchY = panelY + MARGIN;
        int searchW = panelW - MARGIN * 2;
        ctx.fill(searchX, searchY, searchX + searchW, searchY + SEARCH_H, COL_SEARCH_BG);
        ctx.fill(searchX, searchY + SEARCH_H - 1, searchX + searchW, searchY + SEARCH_H, COL_BORDER);
        String display = searchQuery.isEmpty() ? "§7Search blocks..." : searchQuery + "§f_";
        ctx.drawTextWithShadow(tr, display, searchX + 4, searchY + 7, COL_TEXT);

        // Count label
        String countLabel = "§7" + filteredBlocks.size() + " blocks  §8|  §7" + setting.size() + " selected";
        ctx.drawTextWithShadow(tr, countLabel, searchX + 4, searchY + SEARCH_H + 3, COL_TEXT_DIM);

        // Grid area — enable scissor
        ctx.enableScissor(panelX + MARGIN, gridY, panelX + panelW - MARGIN, gridY + gridH);

        int startIdx = scrollOffset * COLS;
        for (int i = 0; i < visibleRows * COLS; i++) {
            int idx = startIdx + i;
            if (idx >= filteredBlocks.size()) break;

            int col = i % COLS;
            int row = i / COLS;
            int cx = panelX + MARGIN + col * CELL;
            int cy = gridY + row * CELL;

            Block block = filteredBlocks.get(idx);
            boolean selected = setting.contains(block);
            boolean hover = mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL;

            // Cell background
            ctx.fill(cx, cy, cx + CELL, cy + CELL, COL_SLOT_BG);

            // Selected highlight
            if (selected) {
                ctx.fill(cx, cy, cx + CELL, cy + CELL, COL_SELECTED);
                // Cyan border for selected
                ctx.fill(cx, cy, cx + CELL, cy + 1, COL_ACCENT);
                ctx.fill(cx, cy + CELL - 1, cx + CELL, cy + CELL, COL_ACCENT);
                ctx.fill(cx, cy, cx + 1, cy + CELL, COL_ACCENT);
                ctx.fill(cx + CELL - 1, cy, cx + CELL, cy + CELL, COL_ACCENT);
            }

            // Hover highlight
            if (hover) {
                ctx.fill(cx, cy, cx + CELL, cy + CELL, COL_HOVER);
            }

            // Block item icon (centered in cell: 16px icon in 20px cell = 2px offset)
            ItemStack stack = new ItemStack(block);
            ctx.drawItem(stack, cx + 2, cy + 2);
        }

        ctx.disableScissor();

        // Scrollbar
        int totalRows = (filteredBlocks.size() + COLS - 1) / COLS;
        if (totalRows > visibleRows) {
            int sbX = panelX + panelW - MARGIN + 1;
            int sbW = 4;
            float ratio = (float) visibleRows / totalRows;
            int barH = Math.max(10, (int) (gridH * ratio));
            int barY = gridY + (int) ((float) scrollOffset / totalRows * gridH);
            ctx.fill(sbX, gridY, sbX + sbW, gridY + gridH, 0xFF111820);
            ctx.fill(sbX, barY, sbX + sbW, barY + barH, COL_ACCENT);
        }

        // Tooltip on hover
        for (int i = 0; i < visibleRows * COLS; i++) {
            int idx = startIdx + i;
            if (idx >= filteredBlocks.size()) break;

            int col = i % COLS;
            int row = i / COLS;
            int cx = panelX + MARGIN + col * CELL;
            int cy = gridY + row * CELL;

            if (mouseX >= cx && mouseX < cx + CELL && mouseY >= cy && mouseY < cy + CELL) {
                Block block = filteredBlocks.get(idx);
                String name = BlockListSetting.getDisplayName(block);
                boolean selected = setting.contains(block);
                String tip = (selected ? "§c[-] " : "§a[+] ") + "§f" + name;
                int tw = tr.getWidth(tip.replaceAll("§.", "")) + 8;
                int tx = mouseX + 12;
                int ty = mouseY - 10;
                if (tx + tw > width) tx = mouseX - tw;
                if (ty < 0) ty = mouseY + 16;
                ctx.fill(tx - 2, ty - 2, tx + tw, ty + 12, 0xF0161B22);
                ctx.fill(tx - 2, ty - 2, tx + tw, ty - 1, COL_ACCENT);
                ctx.drawTextWithShadow(tr, tip, tx + 2, ty, COL_TEXT);
                break;
            }
        }
    }

    // ── Input ────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();

        if (button != 0) return false;

        // Check grid clicks
        int startIdx = scrollOffset * COLS;
        for (int i = 0; i < visibleRows * COLS; i++) {
            int idx = startIdx + i;
            if (idx >= filteredBlocks.size()) break;

            int col = i % COLS;
            int row = i / COLS;
            int cx = panelX + MARGIN + col * CELL;
            int cy = gridY + row * CELL;

            if (mx >= cx && mx < cx + CELL && my >= cy && my < cy + CELL) {
                Block block = filteredBlocks.get(idx);
                setting.toggleBlock(block);
                if (onChanged != null) onChanged.run();
                return true;
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        int totalRows = (filteredBlocks.size() + COLS - 1) / COLS;
        scrollOffset -= (int) vAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalRows - visibleRows)));
        return true;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (charInput.isValidChar()) {
            String s = charInput.asString();
            if (s != null && !s.isEmpty()) {
                searchQuery += s;
                rebuildFilter();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                rebuildFilter();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── Filter ──────────────────────────────────────────────────────────

    private void rebuildFilter() {
        filteredBlocks.clear();
        scrollOffset = 0;
        String q = searchQuery.toLowerCase();
        for (Block block : allBlocks) {
            if (q.isEmpty()) {
                filteredBlocks.add(block);
            } else {
                String path = Registries.BLOCK.getId(block).getPath();
                String displayName = BlockListSetting.getDisplayName(block).toLowerCase();
                if (path.contains(q) || displayName.contains(q)) {
                    filteredBlocks.add(block);
                }
            }
        }
    }
}
