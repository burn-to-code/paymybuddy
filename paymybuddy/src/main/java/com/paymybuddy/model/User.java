package com.paymybuddy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Table(name = "users")
@ToString(exclude = "connections")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(unique = true, nullable = false)
    private String username;

    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(nullable = false)
    private BigDecimal account = BigDecimal.ZERO;

    @ManyToMany
    @JoinTable(name = "user_connections",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "connection_id"))
    private List<User> connections;
}
