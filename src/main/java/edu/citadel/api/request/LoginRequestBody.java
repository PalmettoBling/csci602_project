package edu.citadel.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Body of POST /auth/login. Deliberately unvalidated beyond presence: any
 * further rule here would let a caller distinguish "malformed" from "wrong",
 * and every failed login must look identical from the outside.
 */
@Data
public class LoginRequestBody {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;
}
