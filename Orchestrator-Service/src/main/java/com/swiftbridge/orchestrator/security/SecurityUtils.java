package com.swiftbridge.orchestrator.security;

import com.swiftbridge.orchestrator.exception.UnauthorizedException;
import com.swiftbridge.orchestrator.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

  private final AppUserRepository userRepository;

  public UserPrincipal getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      throw new UnauthorizedException("No authenticated user found");
    }

    UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();

    return userPrincipal;
  }

  public boolean isAdmin() {
    UserPrincipal principal = getCurrentUser();
    return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  public boolean isUser() {
    UserPrincipal principal = getCurrentUser();
    return principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
  }
}
