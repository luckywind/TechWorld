package executors.newCachedThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 * 线程池框架 Executors
 *  newCachedThreadPool  线程池
 *  创建一个可根据需要创建新线程的线程池，但是在以前构造的线程可用时将重用它们。
 *  此连接池没有控制最大并发数 会导致线程数过大 对于大量短暂异步任务的程序来说，使用该线程池能够大大提高性能
 *
 */
public class NewCachedThreadPool {
    public static void main(String[] args) {
        ExecutorService service = Executors.newCachedThreadPool();
        service.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    System.out.println(Thread.currentThread().getName() + ":" + i);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                service.shutdown();
            }
        });
    }
}
