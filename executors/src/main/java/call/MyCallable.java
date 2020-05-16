package call;

import java.util.concurrent.Callable;

/***
 * Callable 实现线程
 */
public class MyCallable implements Callable<String> {

    private long waitTime;

    public MyCallable(long waitTime) {
        this.waitTime = waitTime;
    }

    /***
     * 计算产生结果
     * @return
     * @throws Exception
     */
    @Override
    public String call() throws Exception {
        Thread.sleep(waitTime);
        return Thread.currentThread().getName();
    }

}
