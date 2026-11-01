package com.adrianpmdev.core.features.profile.api.controller;

import com.adrianpmdev.core.features.profile.api.dto.response.ProfileResponse;
import com.adrianpmdev.core.features.profile.application.service.ProfileService;
import com.adrianpmdev.core.security.authentication.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The profile of the caller. There is no route for the profile of somebody else: everybody sees only their own.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ProfileService profileService;

    /**
     * The profile of the caller.
     *
     * @param authentication the caller
     * @return their account and what each game they can open has about them
     */
    @GetMapping("/me")
    public ProfileResponse me(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return profileService.getOwn(user.id(), authorities.contains(ROLE_ADMIN), authorities);
    }
}
