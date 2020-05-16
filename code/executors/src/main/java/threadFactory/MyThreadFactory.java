package threadFactory;

import java.util.List;
import java.util.concurrent.ThreadFactory;

/***
 * 在创建线程的时候，我们当然也能使用工厂模式来生产线程，ThreadFactory是用来实现创建线程的工厂接口，其实它只有一个方法Thread newThread(Runnable r)，所以这个接口没多大用，可以自己编写新接口。
 */
public class MyThreadFactory implements ThreadFactory {

    private  int counter;
    private  String name;
    private List<String> stats;

    @Override
    public Thread newThread(Runnable r) {
        return null;
    }
}
