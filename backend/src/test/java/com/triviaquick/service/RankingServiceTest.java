package com.triviaquick.service;

import com.triviaquick.dto.PodiumDTO;
import com.triviaquick.mapper.RankingMapper;
import com.triviaquick.service.RankingService.RankInput;
import com.triviaquick.service.RankingService.RankedEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RankingServiceTest {

    private static RankInput in(long id, String name, int score, int correct) {
        return new RankInput(id, id + 100, name, score, correct);
    }

    @Test
    void ordersByScoreDescending() {
        List<RankedEntry> r = RankingService.rank(List.of(in(1, "Ana", 500, 2), in(2, "Beto", 900, 3), in(3, "Cris", 700, 3)));
        assertThat(r).extracting(e -> e.input().displayName()).containsExactly("Beto", "Cris", "Ana");
        assertThat(r).extracting(RankedEntry::position).containsExactly(1, 2, 3);
        assertThat(r).noneMatch(RankedEntry::tied);
    }

    @Test
    void equalScoreIsBrokenByCorrectAnswers() {
        List<RankedEntry> r = RankingService.rank(List.of(in(1, "Ana", 800, 2), in(2, "Beto", 800, 4)));
        assertThat(r.get(0).input().displayName()).isEqualTo("Beto");
        assertThat(r).extracting(RankedEntry::position).containsExactly(1, 2);
    }

    @Test
    void fullTieSharesPositionAndSkipsNext() {
        List<RankedEntry> r = RankingService.rank(List.of(
                in(1, "Ana", 1000, 3), in(2, "Beto", 800, 2), in(3, "Cris", 800, 2), in(4, "Dani", 300, 1)));
        assertThat(r).extracting(RankedEntry::position).containsExactly(1, 2, 2, 4);
        assertThat(r).extracting(RankedEntry::tied).containsExactly(false, true, true, false);
    }

    @Test
    void tiedPositionDoesNotDependOnInputOrder() {
        List<RankInput> a = List.of(in(1, "Zoe", 500, 1), in(2, "Al", 500, 1));
        List<RankInput> b = List.of(in(2, "Al", 500, 1), in(1, "Zoe", 500, 1));
        assertThat(RankingService.rank(a)).extracting(RankedEntry::position).containsExactly(1, 1);
        assertThat(RankingService.rank(b)).extracting(e -> e.input().participantId())
                .containsExactlyElementsOf(RankingService.rank(a).stream().map(e -> e.input().participantId()).toList());
    }

    @Test
    void emptyRankingIsEmpty() {
        assertThat(RankingService.rank(List.of())).isEmpty();
        assertThat(RankingService.podium(1L, List.of()).places()).isEmpty();
    }

    @Test
    void podiumGroupsTiesAndSkipsMissingPlaces() {
        var ranking = RankingService.rank(List.of(
                        in(1, "Ana", 900, 3), in(2, "Beto", 900, 3), in(3, "Cris", 400, 1), in(4, "Dani", 100, 0)))
                .stream().map(e -> RankingMapper.toDto(e, 101L)).toList();
        PodiumDTO podium = RankingService.podium(7L, ranking);
        assertThat(podium.places()).extracting(PodiumDTO.PodiumPlaceDTO::position).containsExactly(1, 3);
        assertThat(podium.places().get(0).participants()).hasSize(2);
        assertThat(podium.places().get(1).participants()).extracting(p -> p.displayName()).containsExactly("Cris");
        assertThat(ranking.get(0).currentUser()).isTrue();
    }

    @Test
    void podiumWithSingleParticipant() {
        var ranking = RankingService.rank(List.of(in(1, "Ana", 0, 0))).stream().map(e -> RankingMapper.toDto(e, 1L)).toList();
        assertThat(RankingService.podium(1L, ranking).places()).hasSize(1);
    }

    @Test
    void initialsAreDerivedFromName() {
        assertThat(RankingMapper.initials("ana maría lópez")).isEqualTo("AM");
        assertThat(RankingMapper.initials("Úrsula")).isEqualTo("Ú");
        assertThat(RankingMapper.initials("  ")).isEqualTo("?");
    }
}
