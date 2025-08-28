package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.NewsDTO;

import java.util.List;

public interface NewsService {
    List<NewsDTO> getNews(String searchType);
    void scheduleNews();
}
