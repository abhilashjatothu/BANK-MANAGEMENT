package com.bankms.service;

import com.bankms.model.User;
import com.bankms.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetailsService implementation loading User entities from the database.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new CustomUserDetails(user);
    }

    public static class CustomUserDetails implements UserDetails {

        private final Long userId;
        private final String username;
        private final String password;
        private final boolean enabled;
        private final String roleName;
        private final Long customerId;
        private final String customerName;
        private final Collection<? extends GrantedAuthority> authorities;

        public CustomUserDetails(User user) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.password = user.getPasswordHash();
            this.enabled = user.isEnabled();
            this.roleName = user.getRole().name();
            this.customerId = user.getCustomer() != null ? user.getCustomer().getCustomerId() : null;
            this.customerName = user.getCustomer() != null ? user.getCustomer().getName() : null;
            this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        public Long getUserId() {
            return userId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getRoleName() {
            return roleName;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}
