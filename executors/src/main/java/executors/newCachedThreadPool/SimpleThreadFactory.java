package executors.newCachedThreadPool;

import java.util.concurrent.ThreadFactory;

public class SimpleThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r);
    }
}
