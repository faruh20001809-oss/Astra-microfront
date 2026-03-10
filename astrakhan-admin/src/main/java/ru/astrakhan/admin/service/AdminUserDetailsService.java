package ru.astrakhan.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astrakhan.admin.repository.AdminUserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return adminUserRepository.findByUsername(username)
                .map(au -> new User(
                        au.getUsername(),
                        au.getPasswordHash(),
                        true,
                        true,
                        true,
                        true,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + au.getRole()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
