package com.cxf.springbootquartzdemo.config;

import com.cxf.springbootquartzdemo.jobs.PrintHello;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzSubmitJobs {
    private static final String CRON_EVERY_FIVE_MINUTES = "0 0/5 * ? * * *";

    @Bean(name = "printHello")
    public JobDetailFactoryBean jobMemberStats() {
        return QuartzConfig.createJobDetail(PrintHello.class, "printHello Job");
    }

    @Bean(name = "printHelloTrigger")
    public SimpleTriggerFactoryBean triggerMemberStats(@Qualifier("printHello") JobDetail jobDetail) {
        return QuartzConfig.createTrigger(jobDetail, 60000, "printHello Trigger");
    }
}
