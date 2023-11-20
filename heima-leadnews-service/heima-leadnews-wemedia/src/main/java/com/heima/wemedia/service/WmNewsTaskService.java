package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {
    /**
     * 添加任务到延迟队列中
     * @param id
     * @param publishTime
     */
    void addNewsToTask(Integer id, Date publishTime);

    /**
     * 消费任务，审核文章
     */
    void scanNewsByTask();
}
