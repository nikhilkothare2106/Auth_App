package com.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID id;
    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private String image;
    private boolean enabled;
    private Instant created_at;
    private Instant updated_at;

    @Enumerated(EnumType.STRING)
    private Provider provider;
    private String providerId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        created_at = Instant.now();
        updated_at = created_at;
    }

    @PreUpdate
    protected void onUpdate() {
        updated_at = Instant.now();
    }

    public User(String name, String email, String image, boolean enabled,  Provider provider, String providerId) {
        this.name = name;
        this.email = email;
        this.image = image;
        this.enabled = enabled;
        this.provider = provider;
        this.providerId = providerId;
    }
}
