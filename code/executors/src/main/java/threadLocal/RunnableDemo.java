package threadLocal;

/***
 * ThreadLocal 类用于创建只能由同一个线程读取和写入的线程局部变量。
 * 例如，如果两个线程正在访问引用相同threadLocal变量的代码，那么每个线程都不会看到任何其他线程操作完成的线程变量。
 * 线程与线程之间是数据隔离的
 */
public class RunnableDemo implements  Runnable{
    int counter;
    ThreadLocal<Integer> threadLocalCounter = new ThreadLocal<>();
    @Override
    public void run() {
        counter ++ ;
        if (threadLocalCounter.get()!=null){
            threadLocalCounter.set(threadLocalCounter.get().intValue()+1);
        }else {
            threadLocalCounter.set(0);
        }
        System.out.println("Counter:"+counter);
        System.out.println("threadLocalCounter"+threadLocalCounter.get());
    }
}


