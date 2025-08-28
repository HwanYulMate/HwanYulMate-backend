package com.swyp.api_server.domain.rate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsDTO {
    private String title;
    private String originalLink;
    private String link;
    private String description;
    private String pubDate;

    @Override
    public String toString() {
        return "NewsDTO{" +
                "title='" + title + '\'' +
                ", originalLink='" + originalLink + '\'' +
                ", link='" + link + '\'' +
                ", description='" + description + '\'' +
                ", pubDate='" + pubDate + '\'' +
                '}';
    }
}
