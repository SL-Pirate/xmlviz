package dev.isira.xmlviz.model;

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
        final var min = getMinOccurrences();
        final var max = maxOccurrences;
        if (min == 0 && max == 0) return "0";
        if (min == 0 && max == 1) return "0..1";
        if (min == 0 && max > 1) return "0..*";
        if (min == 1 && max == 1) return "1";
        if (min >= 1 && max > 1) return "1..*";
        return min + ".." + max;
    }
}
