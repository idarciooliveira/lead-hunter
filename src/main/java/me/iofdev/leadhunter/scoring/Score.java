package me.iofdev.leadhunter.scoring;

import java.util.List;

public record Score(int total, List<ScoreItem> items) {

    public static Score of(List<ScoreItem> items) {
        int sum = items.stream().mapToInt(ScoreItem::points).sum();
        return new Score(Math.clamp(sum, 0, 100), List.copyOf(items));
    }
}
