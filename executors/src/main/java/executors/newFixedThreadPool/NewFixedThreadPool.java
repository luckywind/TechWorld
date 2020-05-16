package executors.newFixedThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 * newFixedThreadPool
 * 创建一个可重用固定线程数的线程池，以共享的无界队列方式来运行这些线程。线程池放在 LinkedBlockingQueue 队列
 *
 */
public class NewFixedThreadPool {

    public static void main(String[] args) {
        ExecutorService service = Executors.newFixedThreadPool(10);

        service.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <10 ; i++) {
                    System.out.println(Thread.currentThread().getName()+":"+i);
                }
            }
        });
    }
}
