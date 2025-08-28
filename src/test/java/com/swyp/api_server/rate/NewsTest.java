package com.swyp.api_server.rate;

import com.swyp.api_server.domain.rate.dto.NewsDTO;
import com.swyp.api_server.domain.rate.service.NewsService;
import com.swyp.api_server.domain.rate.service.NewsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
public class NewsTest {

    @Autowired
    private NewsService newsService;
    
    @Test
    void getNewsTest(){
        List<NewsDTO> dtoList = newsService.getNews("KRW");
        System.out.println("리스트 출력");
        for(NewsDTO dto:dtoList){
            System.out.println(dto);
        }
    }

    @Test
    void  getNewsTest2(){
        newsService.scheduleNews();
    }
}
