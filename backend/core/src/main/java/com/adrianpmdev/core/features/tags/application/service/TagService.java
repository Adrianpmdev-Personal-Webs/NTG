package com.adrianpmdev.core.features.tags.application.service;

import com.adrianpmdev.core.features.tags.api.dto.request.CreateTagRequest;
import com.adrianpmdev.core.features.tags.api.dto.request.UpdateTagRequest;
import com.adrianpmdev.core.features.tags.api.dto.response.TagResponse;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.entity.Tag;
import com.adrianpmdev.core.features.users.api.mapper.UserMapper;
import com.adrianpmdev.core.features.tags.infrastructure.persistence.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for managing tags. A tag key such as "pokemon" grants access to that game's data.
 *
 * @author adrianpmdev
 */
@Service
public class TagService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final TagRepository tagRepository;

    /**
     * Builds the service with its dependencies.
     *
     * @param tagRepository tag repository
     */
    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Lists all tags.
     *
     * @return tags ordered by key
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getAll() {
        return tagRepository.findAllByOrderByKeyAsc().stream().map(UserMapper::toResponse).toList();
    }

    /**
     * Creates a tag.
     *
     * @param request tag data
     * @param actorId admin performing the action
     * @return created tag
     */
    @Transactional
    public TagResponse create(CreateTagRequest request, UUID actorId) {
        String key = normalizeKey(request.key());

        if (tagRepository.existsById(key)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag '" + key + "' already exists");
        }

        Tag tag = tagRepository.save(new Tag(key, request.name().trim(), blankToNull(request.description())));
        AUDIT.info("actor={} action=TAG_CREATED tag={}", actorId, key);

        return UserMapper.toResponse(tag);
    }

    /**
     * Updates the name and description of a tag.
     *
     * @param key     tag key
     * @param request new data
     * @param actorId admin performing the action
     * @return updated tag
     */
    @Transactional
    public TagResponse update(String key, UpdateTagRequest request, UUID actorId) {
        Tag tag = find(key);
        tag.setName(request.name().trim());
        tag.setDescription(blankToNull(request.description()));
        AUDIT.info("actor={} action=TAG_UPDATED tag={}", actorId, tag.getKey());

        return UserMapper.toResponse(tag);
    }

    /**
     * Deletes a tag. The database removes it from every user (ON DELETE CASCADE).
     *
     * @param key     tag key
     * @param actorId admin performing the action
     */
    @Transactional
    public void delete(String key, UUID actorId) {
        Tag tag = find(key);
        tagRepository.delete(tag);
        AUDIT.info("actor={} action=TAG_DELETED tag={}", actorId, tag.getKey());
    }

    /**
     * Normalizes a tag key received from a client.
     *
     * @param key raw key
     * @return trimmed lowercase key
     */
    public static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private Tag find(String key) {
        return tagRepository.findById(normalizeKey(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}