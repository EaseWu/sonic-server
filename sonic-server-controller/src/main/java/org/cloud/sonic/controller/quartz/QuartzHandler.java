/*
 *  Copyright (C) [SonicCloudOrg] Sonic Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.cloud.sonic.controller.quartz;

import com.alibaba.fastjson.JSONObject;
import org.cloud.sonic.controller.mapper.JobsMapper;
import org.cloud.sonic.controller.models.domain.Jobs;
import org.cloud.sonic.controller.models.interfaces.JobType;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ZhouYiXun
 * @des quartz处理类
 * @date 2021/8/21 17:08
 */
@Component
public class QuartzHandler {
    private final Logger logger = LoggerFactory.getLogger(QuartzHandler.class);
    @Autowired
    private Scheduler scheduler;
    @Autowired
    private JobsMapper jobsMapper;
    private List<String> typeList = Arrays.asList("cleanFile", "cleanResult", "sendDayReport", "sendWeekReport");

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 创建定时任务
     * @date 2021/8/21 17:40
     */
    public void createScheduleJob(Jobs jobs) throws SchedulerException {
        try {
            Class<? extends Job> jobClass = QuartzJob.class;
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobs.getId() + "").build();
            jobDetail.getJobDataMap().put("id", jobs.getId());
            jobDetail.getJobDataMap().put("type", JobType.TEST_JOB);
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(jobs.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobs.getId() + "").withSchedule(scheduleBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("Create Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Create Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 暂停定时任务
     * @date 2021/8/21 17:42
     */
    public void pauseScheduleJob(Jobs jobs) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobs.getId() + "");
        try {
            scheduler.pauseJob(jobKey);
            logger.info("Pause Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Pause Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 重启定时任务
     * @date 2021/8/21 17:43
     */
    public void resumeScheduleJob(Jobs jobs) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobs.getId() + "");
        try {
            scheduler.resumeJob(jobKey);
            logger.info("Resume Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Resume Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 执行定时任务
     * @date 2021/10/10 12:43
     */
    public void runScheduleJob(Jobs jobs) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobs.getId() + "");
        try {
            scheduler.triggerJob(jobKey);
            logger.info("Run Once Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Run Once Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 更新定时任务
     * @date 2021/8/21 17:43
     */
    public void updateScheduleJob(Jobs jobs) throws SchedulerException {
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(jobs.getId() + "");
            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(jobs.getCronExpression())
                    .withMisfireHandlingInstructionDoNothing();
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            scheduler.rescheduleJob(triggerKey, trigger);
            logger.info("Update Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Update Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    /**
     * @param jobs
     * @return void
     * @author ZhouYiXun
     * @des 删除定时任务
     * @date 2021/8/21 17:44
     */
    public void deleteScheduleJob(Jobs jobs) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobs.getId() + "");
        TriggerKey triggerKey = TriggerKey.triggerKey(jobs.getId() + "");
        try {
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
            logger.info("Delete Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Delete Job failed, cause: " + e.getMessage());
            throw e;
        }
    }

    public CronTrigger getTrigger(Jobs jobs) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobs.getId() + "");
        CronTrigger trigger = null;
        try {
            trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return trigger;
    }

    public List<JSONObject> findSysJobs() {
        List<JSONObject> result = new ArrayList<>();
        for (String type : typeList) {
            TriggerKey triggerKey = TriggerKey.triggerKey(type);
            CronTrigger hasTrigger;
            try {
                hasTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
                if (hasTrigger != null) {
                    JSONObject r = new JSONObject();
                    r.put("type", type);
                    r.put("cron", hasTrigger.getCronExpression());
                    r.put("cronNext", hasTrigger.getNextFireTime());
                    result.add(r);
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public void updateSysScheduleJob(String type, String cron) {
        JobKey jobKey = JobKey.jobKey(type);
        TriggerKey triggerKey = TriggerKey.triggerKey(type);
        try {
            scheduler.pauseTrigger(triggerKey);
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
            logger.info("Delete Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Delete Job failed, cause: " + e.getMessage());
        }
        try {
            Class<? extends Job> jobClass = QuartzJob.class;
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(type).build();
            switch (type) {
                case "cleanFile":
                    jobDetail.getJobDataMap().put("type", JobType.CLEAN_FILE_JOB);
                    if (cron.length() == 0) {
                        cron = "0 0 12 15 * ?";
                    }
                    break;
                case "cleanResult":
                    jobDetail.getJobDataMap().put("type", JobType.CLEAN_RESULT_JOB);
                    if (cron.length() == 0) {
                        cron = "0 0 12 15 * ?";
                    }
                    break;
                case "sendDayReport":
                    jobDetail.getJobDataMap().put("type", JobType.SEND_DAY_REPORT);
                    if (cron.length() == 0) {
                        cron = "0 0 10 * * ?";
                    }
                    break;
                case "sendWeekReport":
                    jobDetail.getJobDataMap().put("type", JobType.SEND_WEEK_REPORT);
                    if (cron.length() == 0) {
                        cron = "0 0 10 ? * Mon";
                    }
                    break;
            }
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron)
                    .withMisfireHandlingInstructionDoNothing();
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(type).withSchedule(scheduleBuilder).build();
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("Create " + type + " System Job Successful!");
        } catch (SchedulerException e) {
            logger.error("Create Job failed, cause: " + e.getMessage());
        }
    }

    public void createSysTrigger() {
        for (String type : typeList) {
//            从数据库中获取数据，然后创建
            Jobs job = jobsMapper.findByType(type);
//            首次部署，初始化系统定时任务
            if (job == null) {
                initSysJob(job, type);
            }
            updateSysScheduleJob(type, job.getCronExpression());
        }
    }

    /**
     * 初始化系统定时任务
     * @param job jobs 表实体
     * @param type 系统定时任务类型
     */
    private void initSysJob(Jobs job, String type) {
        String name = "";
        String cronExpression = "";

        job.setCronExpression(cronExpression);
        job.setName(name);
        job.setProjectId(0);
        job.setStatus(1);
        job.setSuiteId(0);
        job.setType(type);
    }
}