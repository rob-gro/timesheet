package dev.robgro.timesheet.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

class PaginationPlanner {

    private static final Logger log = LoggerFactory.getLogger(PaginationPlanner.class);

    /**
     * Computes page slices for item rows.
     *
     * @param rowHeights  heights of each data row (not including table header)
     * @param avail1      available height for data rows on page 1 (pageH - headerBlock - tableHeaderRow)
     * @param availN      available height for data rows on subsequent pages (pageH - tableHeaderRow)
     * @param minRows     minimum rows per page for anti-orphan (typically 2)
     * @return list of int[2] where each element is {fromItemIndex, toItemIndex} (0-based, inclusive)
     */
    static List<int[]> computeSlices(float[] rowHeights, float avail1, float availN, int minRows) {
        List<int[]> slices = new ArrayList<>();

        if (rowHeights.length == 0) {
            return slices;
        }

        float available = avail1;
        int pageStart = 0;
        float accumulated = 0f;

        for (int i = 0; i < rowHeights.length; i++) {
            if (accumulated + rowHeights[i] > available && i > pageStart) {
                slices.add(new int[]{pageStart, i - 1});
                pageStart = i;
                accumulated = 0f;
                available = availN;
            }
            accumulated += rowHeights[i];
        }
        slices.add(new int[]{pageStart, rowHeights.length - 1});

        applyAntiOrphan(slices, rowHeights, avail1, availN, minRows);

        return slices;
    }

    static float sumHeights(float[] heights, int from, int to) {
        float sum = 0f;
        for (int i = from; i <= to; i++) {
            sum += heights[i];
        }
        return sum;
    }

    private static void applyAntiOrphan(List<int[]> slices, float[] rowHeights,
                                        float avail1, float availN, int minRows) {
        for (int i = slices.size() - 1; i >= 1; i--) {
            int[] curr = slices.get(i);
            int currCount = curr[1] - curr[0] + 1;

            if (currCount >= minRows) continue;

            int[] prev = slices.get(i - 1);
            int prevCount = prev[1] - prev[0] + 1;

            if (prevCount - 1 >= minRows) {
                // steal 1 row from prev
                slices.set(i - 1, new int[]{prev[0], prev[1] - 1});
                slices.set(i, new int[]{prev[1], curr[1]});
                log.debug("Anti-orphan: stole row {} from page {} to page {}", prev[1], i - 1, i);
            } else {
                // merge prev + curr
                int[] merged = new int[]{prev[0], curr[1]};
                slices.remove(i);
                slices.set(i - 1, merged);
                log.debug("Anti-orphan: merged page {} into page {}", i, i - 1);

                // re-split if merged exceeds page capacity AND has enough rows
                float pageAvail = (i - 1 == 0) ? avail1 : availN;
                float mergedHeight = sumHeights(rowHeights, merged[0], merged[1]);
                int mergedCount = merged[1] - merged[0] + 1;

                if (mergedHeight > pageAvail && mergedCount >= 2 * minRows) {
                    int splitAt = Math.max(minRows, Math.min(mergedCount - minRows, mergedCount / 2));
                    slices.set(i - 1, new int[]{merged[0], merged[0] + splitAt - 1});
                    slices.add(i, new int[]{merged[0] + splitAt, merged[1]});
                    log.debug("Anti-orphan: re-split page {} at row offset {}", i - 1, splitAt);
                } else if (mergedHeight > pageAvail) {
                    log.warn("Anti-orphan: merged page {} exceeds capacity but can't split further, accepting", i - 1);
                }
                // loop decrements naturally, no re-check needed
            }
        }
    }
}