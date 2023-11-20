package com.heima.schedule.service.impl;

import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;
    @Test
    void addTask() {

        for (int i=0;i<5;i++){
            Task task = new Task();
            task.setTaskType(100+i);
            task.setPriority(50);
            task.setParameters("task test".getBytes());
            task.setExecuteTime(new Date().getTime()+5000*i);
            long id = taskService.addTask(task);
            System.out.println(id);
        }
    }

    @Test
    public void cancelTask() {
        taskService.cancelTask(1725513198625615874l);
    }

    @Test
    public void testPollTask() {
        Task task = taskService.poll(100, 50);
        System.out.println(task);
    }

    @Autowired
    private CacheService cacheService;

    @Test
    public void testKey() {
        Set<String> keys = cacheService.keys("future_*");
        System.out.println(keys);

        Set<String> scan = cacheService.scan("future_*");
        System.out.println(scan);
    }
}