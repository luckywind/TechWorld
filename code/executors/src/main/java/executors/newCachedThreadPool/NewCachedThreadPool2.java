package executors.newCachedThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/***
 * 线程池框架 Executors
 *  newCachedThreadPool  线程池
 *  创建一个可根据需要创建新线程的线程池，但是在以前构造的线程可用时将重用它们。
 *
 */
public class NewCachedThreadPool2 {

    private ThreadFactory threadFactory = new SimpleThreadFactory();

    public static void main(String[] args) {

        Executors.newCachedThreadPool();

    }
}
