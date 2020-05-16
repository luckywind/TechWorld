package executors.newScheduleThreadPool;

import executors.TestThread2;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *  创建一个单线程执行程序，它可安排在给定延迟后运行命令或者定期地执行。
 */
public class NewScheduleThreadPool {

    public static  void test(){
        ScheduledExecutorService service  =Executors.newScheduledThreadPool(2);
        service.schedule(new TestThread2(),3,TimeUnit.SECONDS);
        service.shutdown();
     }
    public static void main(String[] args) {
         NewScheduleThreadPool.test();
    }
}
