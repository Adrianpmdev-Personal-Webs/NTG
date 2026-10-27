package com.adrianpmdev.pokemon.features.tournaments.infrastructure.persistence.converter;

import com.adrianpmdev.pokemon.features.tournaments.domain.MatchStage;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentFormat;
import com.adrianpmdev.pokemon.features.tournaments.domain.TournamentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converters that store the tournament enums as their lowercase text values (the values the
 * database check constraints accept).
 *
 * @author adrianpmdev
 */
public final class EnumConverters {

    private EnumConverters() {
    }

    /** {@link TournamentFormat} as text. */
    @Converter(autoApply = true)
    public static class FormatConverter implements AttributeConverter<TournamentFormat, String> {

        @Override
        public String convertToDatabaseColumn(TournamentFormat format) {
            return format == null ? null : format.value();
        }

        @Override
        public TournamentFormat convertToEntityAttribute(String value) {
            return value == null ? null : TournamentFormat.fromValue(value);
        }
    }

    /** {@link TournamentStatus} as text. */
    @Converter(autoApply = true)
    public static class StatusConverter implements AttributeConverter<TournamentStatus, String> {

        @Override
        public String convertToDatabaseColumn(TournamentStatus status) {
            return status == null ? null : status.value();
        }

        @Override
        public TournamentStatus convertToEntityAttribute(String value) {
            return value == null ? null : TournamentStatus.fromValue(value);
        }
    }

    /** {@link MatchStage} as text. */
    @Converter(autoApply = true)
    public static class StageConverter implements AttributeConverter<MatchStage, String> {

        @Override
        public String convertToDatabaseColumn(MatchStage stage) {
            return stage == null ? null : stage.value();
        }

        @Override
        public MatchStage convertToEntityAttribute(String value) {
            return value == null ? null : MatchStage.fromValue(value);
        }
    }
}
