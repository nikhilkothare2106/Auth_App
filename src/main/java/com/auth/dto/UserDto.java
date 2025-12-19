package com.auth.dto;

import com.auth.entity.Provider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private UUID id;
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    private String password;
    private String image;
    private Boolean enabled;
    private Instant created_at;
    private Instant updated_at;
    private Provider provider;
    private Set<RoleDto> roles = new HashSet<>();

}
