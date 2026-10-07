package edu.citadel.api.request;

import lombok.Data;

@Data
public class AuthRequestBody {
  private String username;
  private String password;
}
