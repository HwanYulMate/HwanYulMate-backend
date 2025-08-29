package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.NewsDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeNewsListResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.PaginatedNewsResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
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
        return getNewsWithPaging(searchType, 1, 10);
    }
    
    private List<NewsDTO> getNewsWithPaging(String searchType, int start, int display) {
        System.out.println(naverClientId);
        System.out.println(naverClientSecret);
        OkHttpClient client = new OkHttpClient();
        okhttp3.HttpUrl url = okhttp3.HttpUrl.parse("https://openapi.naver.com/v1/search/news.json")
                .newBuilder()
                .addQueryParameter("query", searchType)
                .addQueryParameter("display", String.valueOf(display))
                .addQueryParameter("start", String.valueOf(start))
                .addQueryParameter("sort", "sim")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Naver-Client-Id", naverClientId)
                .addHeader("X-Naver-Client-Secret", naverClientSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                int statusCode = response.code();
                String errorMessage = response.message();
                
                switch (statusCode) {
                    case 400:
                        log.error("네이버 API 요청 오류 (400): {}", errorMessage);
                        break;
                    case 403:
                        log.error("네이버 API 권한 없음 (403): 검색 API 사용 설정 확인 필요");
                        break;
                    case 404:
                        log.error("네이버 API 경로 오류 (404): {}", errorMessage);
                        break;
                    case 500:
                        log.error("네이버 API 서버 오류 (500): {}", errorMessage);
                        break;
                    default:
                        log.error("네이버 API 호출 실패: {} - {}", statusCode, errorMessage);
                }
                return Collections.emptyList();
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
    @Cacheable(value = "exchangeNews", cacheManager = "cacheManager")
    public List<ExchangeNewsListResponseDTO> getExchangeNews() {
        try {
            // "환율" 키워드로 전체 환율 뉴스 조회
            List<NewsDTO> newsList = getNews("환율");
            
            return newsList.stream()
                    .map(this::convertToExchangeNewsResponseDTO)
                    .toList();
                    
        } catch (Exception e) {
            log.error("환율 뉴스 조회 중 오류 발생", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    @Cacheable(value = "currencyNews", key = "#currencyCode", cacheManager = "cacheManager")
    public List<ExchangeNewsListResponseDTO> getCurrencyNews(String currencyCode) {
        try {
            // 통화 코드 유효성 검증
            String currencyName = getCurrencyName(currencyCode);
            
            // 통화명 + "환율" 키워드로 검색
            String searchKeyword = currencyName + " 환율";
            List<NewsDTO> newsList = getNews(searchKeyword);
            
            return newsList.stream()
                    .map(this::convertToExchangeNewsResponseDTO)
                    .toList();
                    
        } catch (Exception e) {
            log.error("통화별 뉴스 조회 중 오류 발생: {}", currencyCode, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * NewsDTO를 ExchangeNewsListResponseDTO로 변환
     */
    private ExchangeNewsListResponseDTO convertToExchangeNewsResponseDTO(NewsDTO newsDTO) {
        return ExchangeNewsListResponseDTO.builder()
                .title(cleanHtmlTags(newsDTO.getTitle()))
                .description(cleanHtmlTags(newsDTO.getDescription()))
                .link(newsDTO.getLink())
                .originalLink(newsDTO.getOriginalLink())
                .pubDate(newsDTO.getPubDate())
                .build();
    }
    
    /**
     * HTML 태그 제거
     */
    private String cleanHtmlTags(String text) {
        if (text == null) return null;
        return text.replaceAll("<[^>]*>", "").trim();
    }
    
    /**
     * 통화 코드에 해당하는 한글 이름 조회
     */
    private String getCurrencyName(String currencyCode) {
        try {
            return ExchangeList.ExchangeType.valueOf(currencyCode.toUpperCase()).getLabel();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 통화 코드입니다: " + currencyCode);
        }
    }

    @Override
    public void scheduleNews(){
        List<String> exchangeList = ExchangeList.ExchangeType.all();
        log.info("테스트 실행");
        log.info(exchangeList.toString());
        for(String exchangeType : exchangeList){
            List<NewsDTO> newsDTOList = getNews(String.valueOf(exchangeType));
            for(NewsDTO newsDTO : newsDTOList){
                log.info(exchangeType + "뉴스 검색" + newsDTO);
            }
        }
    }

    @Override
    public PaginatedNewsResponseDTO getExchangeNewsPaginated(int page, int size) {
        try {
            int start = (page * size) + 1;
            if (start > 1000) {
                return PaginatedNewsResponseDTO.builder()
                        .newsList(Collections.emptyList())
                        .currentPage(page)
                        .pageSize(size)
                        .totalCount(0)
                        .hasNext(false)
                        .build();
            }
            List<NewsDTO> newsList = getNewsWithPaging("환율", start, size);
            
            List<ExchangeNewsListResponseDTO> responseList = newsList.stream()
                    .map(this::convertToExchangeNewsResponseDTO)
                    .toList();
            
            return PaginatedNewsResponseDTO.builder()
                    .newsList(responseList)
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(1000)
                    .hasNext(responseList.size() == size)
                    .build();
                    
        } catch (Exception e) {
            log.error("환율 뉴스 페이징 조회 중 오류 발생", e);
            return PaginatedNewsResponseDTO.builder()
                    .newsList(Collections.emptyList())
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }
    }
    
    @Override
    public PaginatedNewsResponseDTO getCurrencyNewsPaginated(String currencyCode, int page, int size) {
        try {
            String currencyName = getCurrencyName(currencyCode);
            String searchKeyword = currencyName + " 환율";
            
            int start = (page * size) + 1;
            if (start > 1000) {
                return PaginatedNewsResponseDTO.builder()
                        .newsList(Collections.emptyList())
                        .currentPage(page)
                        .pageSize(size)
                        .totalCount(0)
                        .hasNext(false)
                        .build();
            }
            List<NewsDTO> newsList = getNewsWithPaging(searchKeyword, start, size);
            
            List<ExchangeNewsListResponseDTO> responseList = newsList.stream()
                    .map(this::convertToExchangeNewsResponseDTO)
                    .toList();
            
            return PaginatedNewsResponseDTO.builder()
                    .newsList(responseList)
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(1000)
                    .hasNext(responseList.size() == size)
                    .build();
                    
        } catch (Exception e) {
            log.error("통화별 뉴스 페이징 조회 중 오류 발생: {}", currencyCode, e);
            return PaginatedNewsResponseDTO.builder()
                    .newsList(Collections.emptyList())
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }
    }
    
    @Override
    public PaginatedNewsResponseDTO searchExchangeNews(String searchKeyword, int page, int size) {
        try {
            String combinedKeyword = searchKeyword + " 환율";
            
            int start = (page * size) + 1;
            if (start > 1000) {
                return PaginatedNewsResponseDTO.builder()
                        .newsList(Collections.emptyList())
                        .currentPage(page)
                        .pageSize(size)
                        .totalCount(0)
                        .hasNext(false)
                        .build();
            }
            List<NewsDTO> newsList = getNewsWithPaging(combinedKeyword, start, size);
            
            List<ExchangeNewsListResponseDTO> responseList = newsList.stream()
                    .map(this::convertToExchangeNewsResponseDTO)
                    .toList();
            
            return PaginatedNewsResponseDTO.builder()
                    .newsList(responseList)
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(1000)
                    .hasNext(responseList.size() == size)
                    .build();
                    
        } catch (Exception e) {
            log.error("환율 뉴스 검색 중 오류 발생: {}", searchKeyword, e);
            return PaginatedNewsResponseDTO.builder()
                    .newsList(Collections.emptyList())
                    .currentPage(page)
                    .pageSize(size)
                    .totalCount(0)
                    .hasNext(false)
                    .build();
        }
    }
}
