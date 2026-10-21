package com.adrianpmdev.core.features.users.infrastructure.persistence.entity;

import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity from the application core schema.
 * It is linked to the authenticated Supabase user using the same UUID.
 * The email is copied locally when the user is created so it can be listed without calling Supabase.
 *
 * @author adrianpmdev
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "\"user\"", schema = "core")
public class User {
    public static final String DEFAULT_ROLE = "usuario";

    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int EMAIL_MAX_LENGTH = 320;
    private static final int AVATAR_URL_MAX_LENGTH = 500;
    private static final int ROLE_MAX_LENGTH = 30;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "email", nullable = false, length = EMAIL_MAX_LENGTH)
    private String email;

    @NotNull
    @Column(name = "username", nullable = false, length = USERNAME_MAX_LENGTH)
    private String username;

    @Column(name = "avatar_url", length = AVATAR_URL_MAX_LENGTH)
    private String avatarUrl;

    @NotNull
    @ColumnDefault("'usuario'")
    @Column(name = "role", nullable = false, length = ROLE_MAX_LENGTH)
    private String role;

    @ColumnDefault("now()")
    @Column(name = "created_at")
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_tag",
            schema = "core",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_key"))
    private Set<Tag> tags = new HashSet<>();

    /**
     * Creates a regular user linked to an existing Supabase Auth user.
     *
     * @param id       Supabase user id
     * @param email    normalized email
     * @param username unique username
     * @return new user with the default role and no tags
     */
    public static User newRegularUser(UUID id, String email, String username) {
        User user = new User();
        user.id = id;
        user.email = email;
        user.username = username;
        user.role = DEFAULT_ROLE;
        user.createdAt = Instant.now();

        return user;
    }
}