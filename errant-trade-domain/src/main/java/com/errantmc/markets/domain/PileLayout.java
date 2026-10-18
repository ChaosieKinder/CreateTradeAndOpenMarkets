package com.errantmc.markets.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the coins of a pile stand: columns of one denomination each, the most valuable in the
 * centre and the rest in rings around it, all within the block. Pure geometry in block units
 * (centre at 0,0), so it can be computed once per balance and tested without a renderer.
 */
public final class PileLayout {
    private PileLayout() {}

    /** Horizontal scale of one coin item; the renderer draws at this scale. */
    public static final float COIN_SCALE = 0.15f;

    private static final int STACK_HEIGHT_STEP = 10;           // coins per column, grown in steps
    private static final int HUGE_COUNT = 1000;                // beyond this a denomination is one tall column
    private static final int HUGE_COLUMN_HEIGHT = 50;
    private static final float FOOTPRINT_RADIUS = 0.25f * COIN_SCALE; // a coin item is ~0.5 block wide
    private static final float RING_1_RADIUS = 0.18f;
    private static final float RING_STEP = 0.15f;
    private static final float RING_RADIUS_MAX = 0.44f;        // stays inside the block
    private static final float RING_GAP_MIN = 0.06f;
    private static final int[] THICKEN_BY = {4, 3, 2, 5};      // factors a full column is folded by
    private static final int MAX_COLUMNS = maxColumns();

    /**
     * One column of coins.
     * @param height    coins drawn, one per layer
     * @param thickness how many coins each drawn layer stands for (tall columns are folded so
     *                  they draw fewer layers)
     */
    public record Column(CoinDenomination denomination, int height, int thickness, float x, float z) {}

    public static List<Column> of(Purse purse) {
        if (purse.totalCount() <= 0) return List.of();

        // Grow the column height until every column has a place
        int height = 0;
        List<Draft> drafts;
        do {
            height += STACK_HEIGHT_STEP;
            drafts = columns(purse, height);
        } while (drafts.size() > MAX_COLUMNS);
        if (height > STACK_HEIGHT_STEP) thickenFullColumns(drafts, height);

        List<float[]> places = ringPlaces(drafts.size());
        List<Column> out = new ArrayList<>(drafts.size());
        for (int i = 0; i < drafts.size(); i++) {
            Draft d = drafts.get(i);
            out.add(new Column(d.denomination, d.height, d.thickness, places.get(i)[0], places.get(i)[1]));
        }
        return out;
    }

    /** Most valuable first, so it takes the centre. */
    private static List<Draft> columns(Purse purse, int columnHeight) {
        List<Draft> out = new ArrayList<>();
        var coins = CoinDenomination.coins();
        for (int i = coins.size() - 1; i >= 0; i--) {
            CoinDenomination d = coins.get(i);
            int count = purse.count(d);
            if (count > HUGE_COUNT) {
                out.add(new Draft(d, Math.max(columnHeight, HUGE_COLUMN_HEIGHT)));
                continue;
            }
            while (count > 0) {
                int h = Math.min(columnHeight, count);
                out.add(new Draft(d, h));
                count -= h;
            }
        }
        return out;
    }

    /** Folds full columns so they draw fewer, thicker layers. */
    private static void thickenFullColumns(List<Draft> drafts, int fullHeight) {
        for (Draft d : drafts) {
            if (d.height != fullHeight) continue;
            boolean folded = true;
            while (d.height >= STACK_HEIGHT_STEP && folded) {
                folded = false;
                for (int factor : THICKEN_BY) {
                    if (d.height % factor == 0) {
                        d.thickness *= factor;
                        d.height /= factor;
                        folded = true;
                        break;
                    }
                }
            }
        }
    }

    /** The centre, then rings of at least six places each, spaced so columns do not touch. */
    private static List<float[]> ringPlaces(int needed) {
        List<float[]> out = new ArrayList<>(needed);
        if (needed <= 0) return out;
        out.add(new float[]{0f, 0f});
        float spacing = 2f * FOOTPRINT_RADIUS + RING_GAP_MIN;
        for (int ring = 1; out.size() < needed; ring++) {
            float r = Math.min(RING_1_RADIUS + (ring - 1) * RING_STEP, RING_RADIUS_MAX);
            int places = Math.max(6, (int) Math.floor((float) (2 * Math.PI * r) / spacing));
            for (int i = 0; i < places && out.size() < needed; i++) {
                double angle = i * (2 * Math.PI / places);
                out.add(new float[]{(float) Math.cos(angle) * r, (float) Math.sin(angle) * r});
            }
        }
        return out;
    }

    private static int maxColumns() {
        float spacing = 2f * FOOTPRINT_RADIUS + RING_GAP_MIN;
        int rings = 1 + (int) Math.floor((RING_RADIUS_MAX - RING_1_RADIUS) / RING_STEP);
        int total = 1;
        for (int ring = 1; ring <= rings; ring++) {
            float r = Math.min(RING_1_RADIUS + (ring - 1) * RING_STEP, RING_RADIUS_MAX);
            total += Math.max(6, (int) Math.floor((float) (2 * Math.PI * r) / spacing));
        }
        return total;
    }

    private static final class Draft {
        final CoinDenomination denomination;
        int height;
        int thickness = 1;

        Draft(CoinDenomination denomination, int height) {
            this.denomination = denomination;
            this.height = height;
        }
    }
}
