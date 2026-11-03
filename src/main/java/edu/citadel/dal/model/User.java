package edu.citadel.dal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Login name. Nullable because rows created before V4 have no credentials.
     * Uniqueness is enforced by the case-insensitive partial index
     * ux_users_username_lower rather than by a plain column constraint, so it is
     * deliberately not declared unique here.
     */
    @Column(length = 50)
    private String username;

    /**
     * BCrypt hash of the user's password, or null for a row that predates V4.
     *
     * <p>{@code @JsonIgnore} is kept as defence in depth. It originally stopped
     * the since-removed {@code GET /users} from handing out every stored hash,
     * and {@code POST /users} from setting one straight from a request body.
     * No endpoint serializes this entity any more -- {@code /auth/**} answers
     * with {@link edu.citadel.api.response.UserResponse} -- but the annotation
     * stays so that a future controller returning a {@code User} cannot leak the
     * hash by accident. {@code NoSecretDisclosureTest} asserts the outcome
     * against the live routes rather than trusting this annotation alone.
     */
    @JsonIgnore
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    public User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
