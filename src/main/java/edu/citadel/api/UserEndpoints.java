package edu.citadel.api;

import edu.citadel.dal.AccountRepository;
import edu.citadel.dal.UserRepository;
import edu.citadel.dal.model.Account;
import edu.citadel.dal.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserEndpoints {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public UserEndpoints(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> createUser(@RequestBody User user, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setOwnerAccountId(ownerAccountId);
        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<User>> getAllUsers(Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return new ResponseEntity<>(userRepository.findAllByOwnerAccountId(ownerAccountId), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> getById(@PathVariable Long id, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return userRepository.findByIdAndOwnerAccountId(id, ownerAccountId)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User existingUser = userRepository.findByIdAndOwnerAccountId(id, ownerAccountId).orElse(null);

        if (existingUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());

        return new ResponseEntity<>(userRepository.save(existingUser), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Principal principal) {
        Long ownerAccountId = currentAccountId(principal);
        if (ownerAccountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByIdAndOwnerAccountId(id, ownerAccountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private Long currentAccountId(Principal principal) {
        Account account = accountRepository.findAccountByUsername(principal.getName());
        return account == null ? null : account.getUser_id();
    }
}