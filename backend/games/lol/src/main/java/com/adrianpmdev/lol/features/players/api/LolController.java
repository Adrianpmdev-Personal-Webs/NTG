package com.adrianpmdev.lol.features.players.api;

import com.adrianpmdev.lol.features.players.application.LolPlayerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Accounts, history and statistics of League of Legends. Writing is for admins except linking the own account and asking
 * for an update (see LolModule); the admin routes that read check it in the service.
 *
 * @author adrianpmdev
 */
@RestController
@RequestMapping("/api/v1/lol")
@RequiredArgsConstructor
public class LolController {

    private final LolPlayerService service;

    /**
     * A Riot account to link.
     *
     * @param riotId Riot ID as {@code Name#TAG}
     */
    public record LinkRequest(@NotBlank @Size(max = 30) String riotId) {
    }

    @GetMapping("/players")
    public LolResponses.Players players() {
        return service.players();
    }

    @GetMapping("/players/{userId}")
    public LolResponses.PlayerDetail player(@PathVariable UUID userId) {
        return service.detail(userId);
    }

    @PostMapping("/players/{userId}/refresh")
    public LolResponses.PlayerDetail refresh(@PathVariable UUID userId) {
        return service.refresh(userId);
    }

    @PutMapping("/accounts/me")
    public LolResponses.Players linkOwn(@Valid @RequestBody LinkRequest request) {
        return service.linkOwn(request.riotId());
    }

    @DeleteMapping("/accounts/me")
    public LolResponses.Players unlinkOwn() {
        return service.unlinkOwn();
    }

    @GetMapping("/admin/accounts")
    public List<LolResponses.AdminAccount> adminAccounts() {
        return service.adminAccounts();
    }

    @PutMapping("/admin/accounts/{userId}")
    public List<LolResponses.AdminAccount> adminLink(@PathVariable UUID userId, @Valid @RequestBody LinkRequest request) {
        return service.adminLink(userId, request.riotId());
    }

    @DeleteMapping("/admin/accounts/{userId}")
    public List<LolResponses.AdminAccount> adminUnlink(@PathVariable UUID userId) {
        return service.adminUnlink(userId);
    }

    @PostMapping("/admin/accounts/{userId}/download")
    public List<LolResponses.AdminAccount> download(@PathVariable UUID userId) {
        return service.download(userId);
    }

    @PostMapping("/admin/accounts/{userId}/pause")
    public List<LolResponses.AdminAccount> pause(@PathVariable UUID userId) {
        return service.pause(userId);
    }

    @PostMapping("/admin/accounts/download-all")
    public List<LolResponses.AdminAccount> downloadAll() {
        return service.downloadAll();
    }
}
