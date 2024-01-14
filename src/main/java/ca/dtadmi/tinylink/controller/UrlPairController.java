package ca.dtadmi.tinylink.controller;

import ca.dtadmi.tinylink.exceptions.FirestoreExcecutionException;
import ca.dtadmi.tinylink.model.UrlPair;
import ca.dtadmi.tinylink.service.CachingService;
import ca.dtadmi.tinylink.service.CounterService;
import ca.dtadmi.tinylink.service.CryptoService;
import ca.dtadmi.tinylink.service.UrlPairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("")
public class UrlPairController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UrlPairService urlPairService;
    private final CounterService counterService;
    private final CryptoService cryptoService;
    private final CachingService cachingService;

    @Value("${client.base.url}")
    private static String clientBaseUrl;

    @Value("${tiny.link.size}")
    private static int tinyLinkSize;

    public UrlPairController(UrlPairService urlPairService, CounterService counterService, CryptoService cryptoService, CachingService cachingService) {
        this.urlPairService = urlPairService;
        this.counterService = counterService;
        this.cryptoService = cryptoService;
        this.cachingService = cachingService;
    }

    @PostMapping("/shortUrl")
    public ResponseEntity<String> fromLongToShortUrl(@RequestBody String longUrl) {
        try {
            if(longUrl.isBlank()) {
                this.logger.error("Parameter longUrl is empty.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            UrlPair urlPairInDB = this.urlPairService.find(longUrl);
            if(urlPairInDB != null) {
                return new ResponseEntity<>(urlPairInDB.getShortUrl(), HttpStatus.OK);
            }
            int count = this.counterService.getCountFromZookeeper();
            String encryptedValue = this.cryptoService.base62EncodeLong(count);
            String shortUrl = clientBaseUrl + encryptedValue.substring(0, tinyLinkSize);
            UrlPair urlPair = new UrlPair(longUrl, shortUrl);
            this.urlPairService.create(urlPair);
            this.cachingService.evictSingleCacheValue("urlCache", "all");
            return new ResponseEntity<>(shortUrl, HttpStatus.OK);
        } catch (FirestoreExcecutionException e) {
            this.logger.error("Error while getting short url: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.valueOf(e.getErrorCode()));
        }
    }

    @PostMapping("/longUrl")
    public ResponseEntity<String> fromShortToLongUrl(@RequestBody String shortUrl) {
        try {
            if(shortUrl.isBlank()) {
                this.logger.error("Parameter shortUrl is empty.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            UrlPair urlPairInDB = this.urlPairService.findAll().stream().filter(Objects::nonNull).filter(urlPair -> urlPair.getShortUrl().equals(shortUrl)).findFirst().orElse(null);
            if(urlPairInDB != null) {
                return new ResponseEntity<>(urlPairInDB.getShortUrl(), HttpStatus.OK);
            }
            this.logger.error("The provided short url wasn't generated by this system: {}", shortUrl);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (FirestoreExcecutionException e) {
            this.logger.error("Error while getting short url: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.valueOf(e.getErrorCode()));
        }
    }
}
