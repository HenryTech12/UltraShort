package org.app.UltraShort.service;

import jakarta.servlet.http.HttpServletRequest;
import org.app.UltraShort.model.URL;
import org.app.UltraShort.repository.URLRepository;
import org.app.UltraShort.request.URLRequest;
import org.app.UltraShort.response.URLResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class URLService {

    @Autowired
    private URLRepository urlRepository;

    public String generateURLID(String url) {

        String urlID = generateBase64(UUID.randomUUID().toString());

        // If collision, keep generating a NEW one, don't shorten it
        while (urlRepository.findByUrlID(urlID).isPresent()) {
            urlID = generateBase64(UUID.randomUUID().toString());
        }
        return urlID;
    }

    public String generateBase64(String urlId) {
        return Base64.getUrlEncoder().encodeToString(urlId.getBytes())
                .substring(0,7);
    }

    public URLResponse generateShortURL(URLRequest urlRequest, HttpServletRequest request) {
        URL url = new URL();
        url.setUrl(urlRequest.url());

        String urlID = generateURLID(urlRequest.url());
        System.out.println("URL ID: 1".concat(urlID));
        url.setUrlID(urlID);

        String shortUrl = generateApplicationURL(request) +"/"+ urlID;
        url.setShortUrl(shortUrl);

        urlRepository.save(url);
        return new URLResponse(shortUrl, urlID);
    }

    public String generateApplicationURL(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return url.replace(request.getRequestURI(), "");
    }


    @Cacheable(key = "#urlID" , value = "URL")
    public URL fetchURL(String urlID) {
        System.out.println("Time of Initiation: "+System.currentTimeMillis());
        Optional<URL> url = urlRepository
                .findByUrlID(urlID);
        System.out.println("Time of Result: "+System.currentTimeMillis());
        return url.orElse(null);
    }
}
