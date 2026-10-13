package edu.citadel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Iteration 1 -- stateless JWT authentication for the API.
 *
 * <p>Uses the Spring Security 7 component model: a {@link SecurityFilterChain}
 * bean built from {@link HttpSecurity}. There is no WebSecurityConfigurerAdapter,
 * which was removed from Spring Security well before the version this project
 * runs on.
 *
 * <p>Token validation is delegated to the oauth2 resource server filter and the
 * Nimbus {@link JwtDecoder} configured in {@link JwtConfig}. Because that filter
 * sits in the chain itself, every request is checked -- there is no hand-written
 * filter to forget to register, and no path that can quietly bypass verification.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Paths that must answer without a token.
     *
     * <p>/auth/** is how a caller obtains a token in the first place.
     * /verification is called by the React home page before anyone logs in.
     * /health and /info return a fixed string and static info.app.* values, and
     * health.feature asserts both answer unauthenticated.
     * /account/** is asserted unauthenticated by account_creation.feature and
     * account_retrieval.feature.
     * The springdoc paths serve the API documentation UI.
     * /error is Boot's internal error forward: the security filter also runs on
     * ERROR dispatches, so without it a genuine 500 raised inside an
     * authenticated handler would reach the client as a confusing 401.
     */
    private static final String[] PUBLIC_PATHS = {
            "/auth/**",
            "/verification",
            "/health",
            "/info",
            "/actuator/health",
            "/actuator/health/**",
            "/account/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/error"
    };

    @Value("${security.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    private final JsonSecurityErrorHandler errorHandler;

    public SecurityConfig(JsonSecurityErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Safe to disable: the API keeps no session and no cookie, so there
                // is no ambient credential for a cross-site form post to ride on.
                // A bearer token has to be attached deliberately by script.
                .csrf(AbstractHttpConfigurer::disable)
                // No HTML login form and no browser auth dialog. This is a
                // token-only API and either one would answer a 401 with markup.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // A CORS preflight carries no Authorization header by
                        // definition. CorsFilter already short-circuits these, but
                        // permitting them keeps a reordered chain from breaking the
                        // browser client.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // User-scoped routes: the {userId} in the path must equal
                        // the token subject, which is the user id. Declared here
                        // rather than inside a controller so it covers every
                        // current and future /users/{userId}/... route, including
                        // the /users/{userId}/activities endpoint arriving on
                        // feature/exercise-endpoints, with no further change.
                        //
                        // AuthorizationFilter runs ahead of DispatcherServlet, so a
                        // mismatched id is rejected with 403 whether or not a
                        // handler for that path exists yet.
                        .requestMatchers("/users/{userId}/**")
                        .access(new WebExpressionAuthorizationManager(
                                "#userId == authentication.name"))
                        // Everything else -- /users, /activities/** and anything
                        // added later -- needs a valid token. Deny by default, so a
                        // new controller is protected unless somebody opts it out
                        // in PUBLIC_PATHS above.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        // The resource server installs its own bearer-token entry
                        // point, so overriding it here as well as in
                        // exceptionHandling is what actually produces JSON for a
                        // missing, malformed or expired token.
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt at the Spring Security default strength.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Credentials stay off: the browser client sends a bearer token in a
        // header, so no cookie needs to cross the origin boundary.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
