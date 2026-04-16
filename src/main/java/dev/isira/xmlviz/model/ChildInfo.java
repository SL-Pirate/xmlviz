package dev.isira.xmlviz.model;

/**
 * Tracks cardinality of a child element type within a parent element type.
 * Min/max occurrences are computed across all instances of the parent.
 */
public class ChildInfo {
    private int minOccurrences = Integer.MAX_VALUE;
    private int maxOccurrences = 0;
    private int totalOccurrences = 0;
    private int parentInstancesSeen = 0;

    public void recordOccurrence(int count) {
        minOccurrences = Math.min(minOccurrences, count);
        maxOccurrences = Math.max(maxOccurrences, count);
        totalOccurrences += count;
        parentInstancesSeen++;
    }

    public int getMinOccurrences() {
        return minOccurrences == Integer.MAX_VALUE ? 0 : minOccurrences;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public int getTotalOccurrences() {
        return totalOccurrences;
    }

    public int getParentInstancesSeen() {
        return parentInstancesSeen;
    }

    public String getCardinality() {
        int min = getMinOccurrences();
        int max = maxOccurrences;
        if (min == 0 && max == 0) return "0";
        if (min == 0 && max == 1) return "0..1";
        if (min == 0 && max > 1) return "0..*";
        if (min == 1 && max == 1) return "1";
        if (min >= 1 && max > 1) return "1..*";
        return min + ".." + max;
    }
}
