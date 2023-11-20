package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@Slf4j
public class TaskServiceImpl extends ServiceImpl<TaskinfoMapper, Taskinfo> implements TaskService {

    /**
     * 添加延迟任务
     *
     * @param task
     * @return
     */
    @Override
    public long addTask(Task task) {

        //添加任务到数据库中
        boolean flag = addTaskToDb(task);

        //添加任务到redis中
        if (flag) {
            addTaskToCache(task);
        }


        return task.getTaskId();
    }

    @Autowired
    private CacheService cacheService;

    /**
     * 添加任务到redis中
     *
     * @param task
     */
    private void addTaskToCache(Task task) {
        //获取五分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        long nextScheduleTime = calendar.getTimeInMillis();

        String key = task.getTaskType() + "_" + task.getPriority();
        //如果当前任务的执行时间小于等于当前时间，存入list
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        } else if (task.getExecuteTime() <= nextScheduleTime) {
            //如何任务的执行时间大于当前时间并且小于等于预设时间（未来五分钟）存入zSet中
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), nextScheduleTime);
        }

    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;
    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    /**
     * 添加任务到数据库中
     *
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {

        Boolean flag = true;

        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            //设置taskID
            task.setTaskId(taskinfo.getTaskId());

            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }

        return flag;
    }


    /**
     * 取消任务
     *
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        Boolean flag = false;

        //删除任务，更新任务日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);

        //删除redis中的数据
        if (task != null) {
            removeTaskFromCache(task);
            flag = true;
        }

        return flag;
    }

    /**
     * 删除Redis中的数据
     *
     * @param task
     */
    private void removeTaskFromCache(Task task) {

        String key = task.getTaskType() + "_" + task.getPriority();
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            cacheService.lRemove(ScheduleConstants.TOPIC + key, 0, JSON.toJSONString(task));
        } else {
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));
        }
    }

    /**
     * 删除任务，更新任务日志
     *
     * @param taskId
     * @param status
     * @return
     */
    private Task updateDb(long taskId, int status) {
        Task task = null;

        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);
            //更新任务日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);

            //如果是先查出来再删除就会多操作一次数据库
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task);
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (Exception e) {
            log.info("task cancel exception taskId:{}", taskId);
            e.printStackTrace();
        }

        return task;
    }

    /**
     * 按照类型和优先级拉取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;

        try {
            //从redis中拉取任务
            String key = type + "_" + priority;
            String taskJson = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if (StringUtils.isNotBlank(taskJson)) {
                task = JSON.parseObject(taskJson, Task.class);

                //修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("poll task exception");
        }

        return task;
    }

    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh() {

        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        if(StringUtils.isNotBlank(token)){

            log.info("未来数据定时刷新---定时任务");

            Set<String> keys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            for (String key : keys) {
                //获取当前数据的key
                String topicKey = ScheduleConstants.TOPIC + key.split(ScheduleConstants.FUTURE)[1];
                //按照key和分值查询符合条件的数据
                Set<String> tasks = cacheService.zRangeByScore(key, 0, System.currentTimeMillis());

                //同步数据
                if(!tasks.isEmpty()){
                    cacheService.refreshWithPipeline(key,topicKey,tasks);
                    log.info("成功的將"+key+"刷新到"+topicKey);
                }
            }

        }


    }


    /**
     * 数据库任务定时同步到Redis
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @PostConstruct
    public void reloadData(){
        //清理缓存中的数据
        clearCache();

        //查询符合条件的任务，小于未来五分钟的数据
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);
        List<Taskinfo> taskinfos = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, calendar.getTime()));

        //把任务添加到Redis中
        if(taskinfos!=null&& !taskinfos.isEmpty()){
            for (Taskinfo taskinfo : taskinfos) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo,task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }

        log.info("数据库中的任务同步到了Redis中");
    }

    /**
     * 清理缓存中的数据
     */
    private void clearCache() {
        Set<String> topicKey = cacheService.scan(ScheduleConstants.TOPIC+"*");
        Set<String> futureKey = cacheService.scan(ScheduleConstants.FUTURE+"*");
        cacheService.delete(topicKey);
        cacheService.delete(futureKey);
    }
}
