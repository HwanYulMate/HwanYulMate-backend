package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.NewsDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Log4j2
public class NewsServiceImpl implements NewsService {

    @Value("${custom.naver-client-id}")
    private String naverClientId;

    @Value("${custom.naver-client-secret}")
    private String naverClientSecret;


    @Override
    public List<NewsDTO> getNews(String searchType) {
        System.out.println(naverClientId);
        System.out.println(naverClientSecret);
        OkHttpClient client = new OkHttpClient();
        okhttp3.HttpUrl url = okhttp3.HttpUrl.parse("https://openapi.naver.com/v1/search/news.json")
                .newBuilder()
                .addQueryParameter("query", searchType)
                .addQueryParameter("display", "10")
                .addQueryParameter("start", "1")
                .addQueryParameter("sort", "sim")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Naver-Client-Id", naverClientId)
                .addHeader("X-Naver-Client-Secret", naverClientSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                List<NewsDTO> results = new ArrayList<>();
                for (JsonNode item : items) {
                    NewsDTO newsDTO = new NewsDTO();
                    newsDTO.setTitle(item.get("title").asText());
                    newsDTO.setDescription(item.get("description").asText());
                    newsDTO.setOriginalLink(item.get("originallink").asText());
                    newsDTO.setLink(item.get("link").asText());
                    newsDTO.setPubDate(item.get("pubDate").asText());
                    results.add(newsDTO);
                }
                return results;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }


    @Override
    public void scheduleNews(){
        List<String> exchangeList = ExchangeList.ExchangeType.all();
        log.info("테스트 실행");
        log.info(exchangeList);
        for(String exchangeType : exchangeList){
            List<NewsDTO> newsDTOList = getNews(String.valueOf(exchangeType));
            for(NewsDTO newsDTO : newsDTOList){
                log.info(exchangeType + "뉴스 검색" + newsDTO);
            }
        }
    }
}
