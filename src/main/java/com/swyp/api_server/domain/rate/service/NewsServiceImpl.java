package com.swyp.api_server.domain.rate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swyp.api_server.common.constants.Constants;
import com.swyp.api_server.common.http.CommonHttpClient;
import com.swyp.api_server.common.validator.CommonValidator;
import com.swyp.api_server.domain.rate.ExchangeList;
import com.swyp.api_server.domain.rate.dto.NewsDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeNewsListResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.PaginatedNewsResponseDTO;
import com.swyp.api_server.exception.CustomException;
import com.swyp.api_server.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    @Value("${custom.naver-client-id}")
    private String naverClientId;

    @Value("${custom.naver-client-secret}")
    private String naverClientSecret;
    
    private final CommonHttpClient httpClient;
    private final CommonValidator validator;


    @Override
    @Cacheable(value = Constants.Cache.NEWS, key = "#searchType", cacheManager = "cacheManager")
    public List<NewsDTO> getNews(String searchType) {
        return getNewsWithPaging(searchType, 1, Constants.News.DEFAULT_NEWS_SIZE);
    }
    
    private List<NewsDTO> getNewsWithPaging(String searchType, int start, int display) {
        Map<String, String> params = Map.of(
            "query", searchType,
            "display", String.valueOf(display),
            "start", String.valueOf(start),
            "sort", "sim"
        );
        
        String url = httpClient.buildUrl("https://openapi.naver.com/v1/search/news.json", params);
        
        Map<String, String> headers = Map.of(
            "X-Naver-Client-Id", naverClientId,
            "X-Naver-Client-Secret", naverClientSecret
        );

        try {
            JsonNode responseData = httpClient.getJson(url, headers);
            JsonNode items = responseData.get("items");

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

        } catch (Exception e) {
            log.error("뉴스 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.NEWS_API_ERROR, "뉴스 조회 실패: " + e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    @Cacheable(value = Constants.Cache.EXCHANGE_NEWS, cacheManager = "cacheManager")
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
        // 입력 유효성 검증
        validator.validateNewsPageParams(page, size);
        
        try {
            int start = (page * size) + 1;
            if (start > 1000) {
                return createEmptyPaginatedResponse(page, size);
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
            return createEmptyPaginatedResponse(page, size);
        }
    }
    
    @Override
    public PaginatedNewsResponseDTO getCurrencyNewsPaginated(String currencyCode, int page, int size) {
        // 입력 유효성 검증
        validator.validateCurrencyCode(currencyCode);
        validator.validateNewsPageParams(page, size);
        
        try {
            String currencyName = getCurrencyName(currencyCode);
            String searchKeyword = currencyName + " 환율";
            return searchNewsWithPagination(searchKeyword, page, size);
                    
        } catch (Exception e) {
            log.error("통화별 뉴스 페이징 조회 중 오류 발생: {}", currencyCode, e);
            return createEmptyPaginatedResponse(page, size);
        }
    }
    
    @Override
    public PaginatedNewsResponseDTO searchExchangeNews(String searchKeyword, int page, int size) {
        // 입력 유효성 검증
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.NEWS_SEARCH_KEYWORD_EMPTY);
        }
        validator.validateNewsPageParams(page, size);
        
        try {
            String combinedKeyword = searchKeyword + " 환율";
            return searchNewsWithPagination(combinedKeyword, page, size);
                    
        } catch (Exception e) {
            log.error("환율 뉴스 검색 중 오류 발생: {}", searchKeyword, e);
            return createEmptyPaginatedResponse(page, size);
        }
    }
    
    /**
     * 뉴스 검색 및 페이징 처리 공통 메서드
     */
    private PaginatedNewsResponseDTO searchNewsWithPagination(String keyword, int page, int size) {
        int start = (page * size) + 1;
        if (start > 1000) {
            return createEmptyPaginatedResponse(page, size);
        }
        List<NewsDTO> newsList = getNewsWithPaging(keyword, start, size);
        
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
    }

    /**
     * 빈 페이징 응답 생성 (공통 메서드)
     */
    private PaginatedNewsResponseDTO createEmptyPaginatedResponse(int page, int size) {
        return PaginatedNewsResponseDTO.builder()
                .newsList(Collections.emptyList())
                .currentPage(page)
                .pageSize(size)
                .totalCount(0)
                .hasNext(false)
                .build();
    }
}
