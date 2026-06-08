package com.cordillera.bff.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class SignupRequestDTO {
    private String username;
    private String email;
    private String password;
    private Set<String> roles; // Aquí mandarán ["admin"], ["buyer"], etc.
}