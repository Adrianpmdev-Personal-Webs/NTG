package com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.repository;

import com.adrianpmdev.wowforever.features.settings.infrastructure.persistence.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for setting persistence operations.
 *
 * @author adrianpmdev
 */
@Repository
public interface SettingRepository extends JpaRepository<Setting, String> {

    List<Setting> findAllByOrderByKeyAsc();
}
