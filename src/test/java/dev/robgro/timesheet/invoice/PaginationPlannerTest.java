package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationPlannerTest {

    private static final float AVAIL1 = 400f;
    private static final float AVAILN = 600f;
    private static final int   MIN_ROWS = 2;

    // --- Basic cases ---

    @Test
    void shouldReturnEmptyList_whenNoRows() {
        List<int[]> slices = PaginationPlanner.computeSlices(new float[0], AVAIL1, AVAILN, MIN_ROWS);
        assertThat(slices).isEmpty();
    }

    @Test
    void shouldReturnSingleSlice_whenAllRowsFitOnPage1() {
        float[] heights = {50f, 50f, 50f}; // total 150f < AVAIL1=400f
        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        assertThat(slices).hasSize(1);
        assertThat(slices.get(0)).containsExactly(0, 2);
    }

    @Test
    void shouldSplitAcrossPages_whenRowsExceedPage1() {
        // 9 rows of 60f each: total=540f > AVAIL1=400f → split
        float[] heights = new float[9];
        java.util.Arrays.fill(heights, 60f);

        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        assertThat(slices).hasSizeGreaterThanOrEqualTo(2);
        // First slice fits within AVAIL1
        int[] s0 = slices.get(0);
        assertThat(PaginationPlanner.sumHeights(heights, s0[0], s0[1])).isLessThanOrEqualTo(AVAIL1);
        // Subsequent slices fit within AVAILN
        for (int i = 1; i < slices.size(); i++) {
            int[] s = slices.get(i);
            assertThat(PaginationPlanner.sumHeights(heights, s[0], s[1])).isLessThanOrEqualTo(AVAILN);
        }
        // All rows covered exactly once
        assertAllRowsCovered(slices, heights.length);
    }

    // --- Anti-orphan: steal ---

    @Test
    void shouldPreventOrphanRow_whenLastPageWouldHaveOneRow() {
        // 7 rows of 80f each: avail1=400 → 5 fit on p1 (5*80=400), 2 on p2 (2*80=160 < 600)
        // Wait - 5*80=400 = avail1 exactly, but 6th row pushes it over. Let me use a scenario
        // where natural split gives last page 1 row: 7 rows of 70f
        // p1: 5 rows = 350f <= 400f, 6th would be 420f > 400f → p1=[0,4], p2=[5,6] (2 rows) → no orphan
        // Let me force 1-row last page: AVAIL1=400, rows=[80,80,80,80,80,90]
        // p1: 0..3=320<=400, add 80→400 exactly, try add 90→490>400 → p1=[0,4]? No:
        // i=0: acc=80, i=1:acc=160, i=2:acc=240, i=3:acc=320, i=4:acc=400, i=5: acc+90=490>400 → slice[0,4], pageStart=5, acc=90
        // last slice: [5,5] → 1 row → orphan!
        float[] heights = {80f, 80f, 80f, 80f, 80f, 90f};

        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        // Anti-orphan should ensure last page has >= MIN_ROWS
        for (int[] s : slices) {
            int count = s[1] - s[0] + 1;
            // Only single-item invoices are exempt (slices.size()==1 means no previous page to steal from)
            if (slices.size() > 1) {
                assertThat(count).isGreaterThanOrEqualTo(MIN_ROWS)
                        .withFailMessage("Page slice [%d..%d] has only %d row(s), expected >= %d", s[0], s[1], count, MIN_ROWS);
            }
        }
        assertAllRowsCovered(slices, heights.length);
    }

    // --- Anti-orphan: merge ---

    @Test
    void shouldMergeSlices_whenPrevCantLoseRowWithoutOrphan() {
        // p1 has exactly MIN_ROWS=2, last page has 1 row → can't steal from prev (would leave 1)
        // So they merge. avail1=400, rows: [200,200,100]
        // i=0: acc=200, i=1: acc=400, i=2: acc+100=500>400 → p1=[0,1], last=[2,2] (1 row)
        // prev has 2 rows, prev-1=1 < MIN_ROWS=2 → can't steal → MERGE
        float[] heights = {200f, 200f, 100f};

        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        // After merge: p1=[0,2] (3 rows, within AVAILN=600? 500<=600 yes)
        assertThat(slices).hasSize(1);
        assertThat(slices.get(0)).containsExactly(0, 2);
    }

    // --- Single item ---

    @Test
    void shouldHandleSingleItemInvoice() {
        float[] heights = {100f};

        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        // Single item: 1 slice, 1 row — anti-orphan loop starts at i>=1 so never runs
        assertThat(slices).hasSize(1);
        assertThat(slices.get(0)).containsExactly(0, 0);
    }

    // --- Row index tracking after anti-orphan ---

    @Test
    void shouldCorrectlyTrackRowNumbersAfterAntiOrphan() {
        // 6 rows: rows 0-4 on p1 (5*80=400=avail1), row 5 on p2 (1 row → orphan)
        // After steal: p1=[0,3], p2=[4,5]
        float[] heights = {80f, 80f, 80f, 80f, 80f, 80f};

        List<int[]> slices = PaginationPlanner.computeSlices(heights, AVAIL1, AVAILN, MIN_ROWS);

        assertThat(slices).hasSize(2);
        int[] p1 = slices.get(0);
        int[] p2 = slices.get(1);

        // After steal: p1=[0,3], p2=[4,5]
        // p2 starts exactly where p1 ends + 1 (contiguous coverage)
        assertThat(p2[0]).isEqualTo(p1[1] + 1)
                .withFailMessage("p2 should follow p1 contiguously; p1=[%d..%d], p2=[%d..%d]",
                        p1[0], p1[1], p2[0], p2[1]);
        // All 6 rows covered
        assertAllRowsCovered(slices, heights.length);
        // Each page has >= MIN_ROWS after anti-orphan
        assertThat(p1[1] - p1[0] + 1).isGreaterThanOrEqualTo(MIN_ROWS);
        assertThat(p2[1] - p2[0] + 1).isGreaterThanOrEqualTo(MIN_ROWS);
    }

    // --- Helper ---

    private void assertAllRowsCovered(List<int[]> slices, int totalRows) {
        // Verify contiguous, non-overlapping coverage of [0..totalRows-1]
        assertThat(slices).isNotEmpty();
        assertThat(slices.get(0)[0]).isEqualTo(0);
        assertThat(slices.get(slices.size() - 1)[1]).isEqualTo(totalRows - 1);
        for (int i = 1; i < slices.size(); i++) {
            assertThat(slices.get(i)[0]).isEqualTo(slices.get(i - 1)[1] + 1)
                    .withFailMessage("Gap or overlap between slice %d and %d", i - 1, i);
        }
    }
}
