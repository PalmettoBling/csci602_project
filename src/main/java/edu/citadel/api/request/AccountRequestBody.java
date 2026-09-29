package edu.citadel.api.request;

import lombok.Data;

@Data
public class AccountRequestBody {
  private String username;
  private String password;
  private String email;
}
