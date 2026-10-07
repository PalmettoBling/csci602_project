package edu.citadel.api;

import edu.citadel.api.request.AccountRequestBody;
import edu.citadel.api.request.AuthRequestBody;
import edu.citadel.api.response.AuthResponse;
import edu.citadel.api.response.ErrorResponse;
import edu.citadel.config.JwtService;
import edu.citadel.dal.AccountRepository;
import edu.citadel.dal.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user registration and login, issuing JWT access tokens on success.
 * These endpoints are intentionally unauthenticated so that new clients can
 * obtain a token in the first place.
 */
@RestController
@RequestMapping("/auth")
public class AuthEndpoints {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Autowired
    public AuthEndpoints(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody AccountRequestBody accountRequestBody) {
        if (accountRepository.findAccountByUsername(accountRequestBody.getUsername()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse("Username already exists"));
        }

        Account account = new Account();
        account.setUsername(accountRequestBody.getUsername());
        account.setEmail(accountRequestBody.getEmail());
        account.setPassword(passwordEncoder.encode(accountRequestBody.getPassword()));

        Account savedAccount = accountRepository.save(account);
        String token = jwtService.generateToken(savedAccount.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, savedAccount.getUsername()));
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody AuthRequestBody authRequestBody) {
        Account account = accountRepository.findAccountByUsername(authRequestBody.getUsername());

        if (account == null || !passwordEncoder.matches(authRequestBody.getPassword(), account.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid username or password"));
        }

        String token = jwtService.generateToken(account.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, account.getUsername()));
    }
}
