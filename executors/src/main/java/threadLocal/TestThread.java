package threadLocal;

public class TestThread {
    public static void main(String[] args) {
        RunnableDemo runnableDemo = new RunnableDemo();

        Thread t1 = new Thread(runnableDemo); //线程1
        Thread t2 = new Thread(runnableDemo);//线程2
        Thread t3 = new Thread(runnableDemo);//线程3
        Thread t4 = new Thread(runnableDemo);//线程4

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
