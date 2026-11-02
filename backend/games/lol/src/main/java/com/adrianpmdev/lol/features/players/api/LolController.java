package com.adrianpmdev.lol.features.players.api;

import com.adrianpmdev.lol.features.players.application.LolPlayerService;
import com.adrianpmdev.platform.error.ApiErrors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    /**
     * The day a season starts.
     *
     * @param date first day, as yyyy-MM-dd
     */
    public record SeasonRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String date) {
    }

    @GetMapping("/admin/seasons")
    public List<LolResponses.AdminSeason> adminSeasons() {
        return service.adminSeasons();
    }

    @PutMapping("/admin/seasons/{key}")
    public List<LolResponses.AdminSeason> saveSeason(@PathVariable String key,
                                                     @Valid @RequestBody SeasonRequest request) {
        if (!key.matches("\\d{2}")) {
            throw ApiErrors.badRequest("La temporada debe ser un número de dos cifras, como 27");
        }

        try {
            return service.saveSeason(key, LocalDate.parse(request.date()));
        } catch (DateTimeParseException exception) {
            throw ApiErrors.badRequest("Esa fecha no existe");
        }
    }

    @DeleteMapping("/admin/seasons/{key}")
    public List<LolResponses.AdminSeason> deleteSeason(@PathVariable String key) {
        return service.deleteSeason(key);
    }

    @GetMapping("/players")
    public LolResponses.Players players() {
        return service.players();
    }

    @GetMapping("/players/{userId}")
    public LolResponses.PlayerDetail player(@PathVariable UUID userId,
                                            @RequestParam(required = false) String season) {
        return service.detail(userId, season);
    }

    @PostMapping("/players/{userId}/refresh")
    public LolResponses.PlayerDetail refresh(@PathVariable UUID userId,
                                             @RequestParam(required = false) String season) {
        return service.refresh(userId, season);
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

    @PostMapping("/admin/accounts/{userId}/download-again")
    public List<LolResponses.AdminAccount> downloadAgain(@PathVariable UUID userId) {
        return service.downloadAgain(userId);
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
