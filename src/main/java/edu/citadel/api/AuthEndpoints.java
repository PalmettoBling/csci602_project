package edu.citadel.api;

import edu.citadel.api.request.LoginRequestBody;
import edu.citadel.api.request.RegisterRequestBody;
import edu.citadel.api.response.AuthResponse;
import edu.citadel.api.response.UserResponse;
import edu.citadel.auth.DuplicateUserException;
import edu.citadel.auth.InvalidCredentialsException;
import edu.citadel.auth.TokenService;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.User;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration and login. Both paths are public (see
 * {@link edu.citadel.config.SecurityConfig}) because they are how a caller gets
 * a token in the first place.
 */
@RestController
@RequestMapping("/auth")
public class AuthEndpoints {

    /**
     * A valid BCrypt hash, compared against when the username is unknown so that
     * a missing user costs the same hashing work as a wrong password. Without it,
     * response timing reveals which usernames exist. The plaintext behind it is
     * irrelevant and it is never a credential for anything -- no account can hold
     * this value, because registration always hashes a freshly supplied password.
     */
    private static final String UNKNOWN_USER_DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthEndpoints(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Creates a user with a BCrypt-hashed password.
     *
     * <p>Answers 201 with the new user, 400 if any required field is missing or
     * malformed, and 409 if the username or email is taken. The response is a
     * {@link UserResponse}, never the entity, so the hash cannot escape.
     */
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequestBody request) {

        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        // Checked up front so the common case is a clean 409 rather than a
        // constraint violation surfacing from the driver.
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserException("That username is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserException("That email address is already registered.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        try {
            User saved = userRepository.saveAndFlush(user);
            return new ResponseEntity<>(UserResponse.from(saved), HttpStatus.CREATED);
        } catch (DataIntegrityViolationException duplicateSlippedThrough) {
            // Two concurrent registrations for the same name: the exists checks
            // above both passed and the unique index caught it. Same answer as if
            // the check had caught it, so the client sees consistent behaviour.
            throw new DuplicateUserException("That username or email is already registered.");
        }
    }

    /**
     * Exchanges credentials for a signed JWT.
     *
     * <p>Answers 200 with the token, or 401 with a JSON body. Every failure mode
     * takes the same branch and costs roughly the same time, because
     * {@link PasswordEncoder#matches} is still called on a dummy hash when the
     * user does not exist.
     */
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequestBody request) {

        User user = userRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElse(null);

        // A user row created before V4, or through POST /users, has no hash. Such
        // an account cannot be logged into; it must not be treated as a match.
        String storedHash = (user == null) ? null : user.getPasswordHash();

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                storedHash != null ? storedHash : UNKNOWN_USER_DUMMY_HASH);

        if (user == null || storedHash == null || !passwordMatches) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        String token = tokenService.issueToken(user);

        return ResponseEntity.ok(new AuthResponse(
                token,
                "Bearer",
                tokenService.getExpirationSeconds(),
                user.getId(),
                user.getUsername()));
    }
}
