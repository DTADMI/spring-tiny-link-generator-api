package ca.dtadmi.tinylink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlPair {

    private String longUrl;
    private String shortUrl;

    public UrlPair(UrlPair urlPair) {
        this.longUrl = urlPair.longUrl;
        this.shortUrl = urlPair.shortUrl;
    }
}