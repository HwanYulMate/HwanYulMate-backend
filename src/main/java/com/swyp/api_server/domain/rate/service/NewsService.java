package com.swyp.api_server.domain.rate.service;

import com.swyp.api_server.domain.rate.dto.NewsDTO;
import com.swyp.api_server.domain.rate.dto.response.ExchangeNewsListResponseDTO;
import com.swyp.api_server.domain.rate.dto.response.PaginatedNewsResponseDTO;

import java.util.List;

/**
 * 뉴스 서비스 인터페이스
 * - 환율 관련 뉴스 조회 및 관리
 */
public interface NewsService {
    
    /**
     * 검색 타입별 뉴스 조회 (기존 메서드)
     * @param searchType 검색 타입
     * @return 뉴스 목록
     */
    List<NewsDTO> getNews(String searchType);
    
    /**
     * 환율 관련 전체 뉴스 조회
     * @return 환율 뉴스 목록
     */
    List<ExchangeNewsListResponseDTO> getExchangeNews();
    
    /**
     * 특정 통화 관련 뉴스 조회
     * @param currencyCode 통화 코드
     * @return 통화별 뉴스 목록
     */
    List<ExchangeNewsListResponseDTO> getCurrencyNews(String currencyCode);
    
    /**
     * 뉴스 스케줄링 (기존 메서드)
     */
    void scheduleNews();
    
    /**
     * 환율 관련 뉴스 페이징 조회
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 환율 뉴스 목록
     */
    PaginatedNewsResponseDTO getExchangeNewsPaginated(int page, int size);
    
    /**
     * 특정 통화 관련 뉴스 페이징 조회
     * @param currencyCode 통화 코드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 페이징된 통화별 뉴스 목록
     */
    PaginatedNewsResponseDTO getCurrencyNewsPaginated(String currencyCode, int page, int size);
    
    /**
     * 환율 뉴스 검색 (페이징)
     * @param searchKeyword 검색 키워드
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색된 환율 뉴스 목록
     */
    PaginatedNewsResponseDTO searchExchangeNews(String searchKeyword, int page, int size);
}
