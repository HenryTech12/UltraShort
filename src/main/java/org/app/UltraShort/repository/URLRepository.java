package org.app.UltraShort.repository;

import org.app.UltraShort.model.URL;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface URLRepository extends JpaRepository<URL, Long> {
    Optional<URL> findByShortUrl(String shortUrl);
    Optional<URL> findByUrlID(String urlID);
}
