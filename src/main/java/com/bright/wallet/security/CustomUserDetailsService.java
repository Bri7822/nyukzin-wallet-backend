package com.bright.wallet.security;

import com.bright.wallet.model.User;
import com.bright.wallet.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));

        // Before: new ArrayList<>() — no roles, empty authorities
        // Now: we pass the actual role from the DB
        // SimpleGrantedAuthority wraps the role string into the format Spring expects
        // Spring Security reads this to decide what the user is allowed to do
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole()))
                // e.g. SimpleGrantedAuthority("ROLE_ADMIN")
                // or   SimpleGrantedAuthority("ROLE_USER")
        );
    }
}