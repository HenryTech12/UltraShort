package org.app.UltraShort.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.app.UltraShort.exceptions.URLNotFoundException;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.repository.URLRepository;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class URLService {

    @Autowired
    private URLRepository urlRepository;

    @Autowired
    private RedisTemplate<String, URL> redisTemplate;


    public URLResponse createShortURL(URLRequest urlRequest, HttpServletRequest request) {
        URL url = new URL();
        url.setUrl(urlRequest.url());
        String urlID = HashService.generateHash(urlRequest.url());

        try {

            if (urlRepository.findByUrlID(urlID).isPresent()) {
                urlID = HashService.generateHash(urlRequest.url().
                        concat(String.valueOf(System.currentTimeMillis())));
            }
            url.setUrlID(urlID);
            url.setShortUrl(generateShortURL(request, urlID));
            url.setUrl(urlRequest.url());
            urlRepository.save(url);

        }
        catch(DataIntegrityViolationException e) {
            urlID = HashService.generateHash(urlRequest.url().
                    concat(String.valueOf(System.currentTimeMillis())));
            url.setUrlID(urlID);
            url.setShortUrl(generateShortURL(request,urlID));
            url.setUrl(urlRequest.url());
            urlRepository.save(url);
        }
        //save to redis for fast check
        long ttl = new Random().nextInt(10) + 60;
        redisTemplate.opsForValue().
                set(urlID, url,Duration.ofSeconds(ttl));
       return new URLResponse(url.getShortUrl(), urlID);
    }

    public String generateApplicationURL(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return url.replace(request.getRequestURI(), "");
    }

    private String generateShortURL(HttpServletRequest request, String urlID) {
        return generateApplicationURL(request) +"/"+ urlID;
    }


    public URL fetchURL(String urlID) {
        URL url = redisTemplate.opsForValue().get(urlID);
        if(url != null) {
            log.info("Cache hit for URL ID {}, returning from Redis", urlID);
            return url;
        } else {
            log.info("Cache miss for URL ID {}, fetching from database", urlID);
            System.out.println("Time of Initiation: "+System.currentTimeMillis());

            url = urlRepository
                    .findByUrlID(urlID).orElseThrow(() -> new URLNotFoundException("URL not found"));
            long ttl = new Random().nextInt(10) + 60;
            Boolean redisOutput = redisTemplate.opsForValue().setIfAbsent(urlID, url, Duration.ofSeconds(ttl));
            System.out.println("Time of Result: " + System.currentTimeMillis());

            if(redisOutput) {
                log.info("URL with ID {} cached in Redis for {} seconds", urlID, ttl);
            }
            else {
                log.info("URL with ID {} already exists in Redis, skipping cache", urlID);
            }
        }
       return url;
    }
}
