package com.heima.model.article.dtos;

import com.heima.model.article.pojos.ApArticle;
import lombok.Data;

@Data
public class ArticleDto extends ApArticle {

    /**
     * 进行属性拓展
     * 文章内容
     */
    private String content;
}
