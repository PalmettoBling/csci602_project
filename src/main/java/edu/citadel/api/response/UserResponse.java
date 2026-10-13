package edu.citadel.api.response;

import edu.citadel.dal.model.User;

/**
 * Public view of a user. Exists so that no endpoint ever has to serialize the
 * entity -- and therefore the password hash -- back to a client.
 */
public record UserResponse(Long id, String name, String username, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getUsername(), user.getEmail());
    }
}
