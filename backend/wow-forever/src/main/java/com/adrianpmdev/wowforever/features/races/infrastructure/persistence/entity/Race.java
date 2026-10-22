package com.adrianpmdev.wowforever.features.races.infrastructure.persistence.entity;

import com.adrianpmdev.wowforever.features.classes.infrastructure.persistence.entity.WowClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Playable race of WoW Forever. The classes it can play are stored in the race_class join table.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "race", schema = "wow_forever")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(length = 40, nullable = false, unique = true)
    private String slug;

    @Column(length = 60, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private Faction faction;

    @Column(length = 100, nullable = false)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "race_class",
            schema = "wow_forever",
            joinColumns = @JoinColumn(name = "race_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id"))
    @ToString.Exclude
    private Set<WowClass> classes = new HashSet<>();

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = Instant.now();
    }
}
