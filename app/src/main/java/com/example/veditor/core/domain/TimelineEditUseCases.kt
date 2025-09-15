package com.example.veditor.core.domain

import com.example.veditor.core.model.TimeMs
import com.example.veditor.core.model.TimeRange
import com.example.veditor.core.model.Timeline

/** 특정 클립의 범위를 원본 범위 내에서 조정한다. */
class TrimClipUseCase {
    operator fun invoke(timeline: Timeline, clipIndex: Int, newRange: TimeRange): Timeline {
        require(clipIndex in timeline.clips.indices) { "invalid clip index" }
        val original = timeline.clips[clipIndex]
        require(
            newRange.startMs.value >= original.range.startMs.value && newRange.endMs.value <= original.range.endMs.value,
        ) {
            "newRange must be within original clip range"
        }
        // 인접 클립과의 겹침을 방지
        if (clipIndex > 0) {
            val prev = timeline.clips[clipIndex - 1]
            require(prev.range.endMs.value <= newRange.startMs.value) { "trim overlaps previous clip" }
        }
        if (clipIndex < timeline.clips.lastIndex) {
            val next = timeline.clips[clipIndex + 1]
            require(newRange.endMs.value <= next.range.startMs.value) { "trim overlaps next clip" }
        }
        val updated = timeline.clips.toMutableList()
        updated[clipIndex] = original.copy(range = newRange)
        return timeline.copy(clips = updated)
    }
}

/** 특정 시점에서 하나의 클립을 두 개로 분할한다. */
class SplitClipUseCase {
    operator fun invoke(timeline: Timeline, clipIndex: Int, splitAt: TimeMs): Timeline {
        require(clipIndex in timeline.clips.indices) { "invalid clip index" }
        val target = timeline.clips[clipIndex]
        require(splitAt.value > target.range.startMs.value && splitAt.value < target.range.endMs.value) {
            "splitAt must be inside clip"
        }

        val first = target.copy(
            id = target.id + "_a",
            range = TimeRange(target.range.startMs, TimeMs(splitAt.value)),
        )
        val second = target.copy(
            id = target.id + "_b",
            range = TimeRange(TimeMs(splitAt.value), target.range.endMs),
        )

        val newClips = buildList {
            addAll(timeline.clips.take(clipIndex))
            add(first)
            add(second)
            addAll(timeline.clips.drop(clipIndex + 1))
        }
        return timeline.copy(clips = newClips)
    }
}

/** 인접한 두 클립을 병합한다. 같은 소스여야 한다. */
class MergeAdjacentClipsUseCase {
    operator fun invoke(timeline: Timeline, firstIndex: Int): Timeline {
        require(firstIndex in timeline.clips.indices && firstIndex < timeline.clips.lastIndex) { "invalid indices" }
        val a = timeline.clips[firstIndex]
        val b = timeline.clips[firstIndex + 1]
        require(a.sourceUri == b.sourceUri) { "cannot merge clips from different sources" }
        require(a.range.endMs.value == b.range.startMs.value) { "clips must be adjacent" }

        val merged = a.copy(
            id = a.id + "+" + b.id,
            range = TimeRange(a.range.startMs, b.range.endMs),
        )
        val newClips = buildList {
            addAll(timeline.clips.take(firstIndex))
            add(merged)
            addAll(timeline.clips.drop(firstIndex + 2))
        }
        return timeline.copy(clips = newClips)
    }
}
