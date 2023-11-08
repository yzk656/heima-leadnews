package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {

    /**
     * 生成静态文件上传到Minio中
     * @param apArticle
     * @param content
     */
    public void buildArticleToMinio(ApArticle apArticle,String content);
}
