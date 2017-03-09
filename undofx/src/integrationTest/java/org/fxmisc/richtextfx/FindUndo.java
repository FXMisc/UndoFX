package org.fxmisc.richtextfx;

import javafx.scene.control.IndexRange;
import org.reactfx.util.Tuple2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.reactfx.util.Tuples.t;

public class FindUndo {

    public static List<IndexRange> findAllValidUndoRanges(TextChange bubblyUndo, List<TextChange> dependencies) {
        List<SplittableIndexRange> currentRanges = new ArrayList<>(1);
        currentRanges.add(new SplittableIndexRange(bubblyUndo.getStart(), bubblyUndo.insertedEndPosition()));

        for (TextChange dependency : dependencies) {
            currentRanges = currentRanges.stream()
                    .map(r -> r.split(dependency.getStart(), dependency.removedEndPosition()))
                    .flatMap(t -> Stream.of(t._1, t._2))
                    .filter(r -> r.getLength() != 0)
                    .collect(Collectors.toList());
        }
        return currentRanges.stream().map(SplittableIndexRange::asIndexRange).collect(Collectors.toList());
    }

}

class SplittableIndexRange {

    private static final SplittableIndexRange EMPTY_RANGE = new SplittableIndexRange(0, 0);

    private final int start;
    public final int getStart() { return start; }

    private final int end;
    public final int getEnd() { return end; }

    public final int getLength() { return end - start; }

    SplittableIndexRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    Tuple2<SplittableIndexRange, SplittableIndexRange> split(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("'From' must be less than or equal to 'to'");
        }

        if (outsideOfRangeBounds(from, to)) {
            return t(this, EMPTY_RANGE);
        }
        SplittableIndexRange left = updateEnd(from);
        SplittableIndexRange right = updateStart(to);

        return t(left, right);
    }

    private boolean outsideOfRangeBounds(int from, int to) {
        return to < start || end < from;
    }

    IndexRange asIndexRange() {
        return new IndexRange(start, end);
    }

    private SplittableIndexRange updateEnd(int end) {
        return updatedOrEmpty(start, end);
    }

    private SplittableIndexRange updateStart(int start) {
        return updatedOrEmpty(start, end);
    }

    private SplittableIndexRange updatedOrEmpty(int start, int end) {
        return start == end
                ? EMPTY_RANGE
                : new SplittableIndexRange(start, end);
    }

    @Override
    public String toString() {
        return String.format("SplittableIndexRange(start=%s end=%s length=%s", start, end, getLength());
    }
}
