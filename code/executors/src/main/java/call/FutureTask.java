package call;

import java.util.concurrent.*;

public class FutureTask {

    public static void main(String[] args) {
        MyCallable myCallable = new MyCallable(10000);
        MyCallable myCallable1 = new MyCallable(2000);

        //futureTask 计算结果
        java.util.concurrent.FutureTask<String> futureTask = new java.util.concurrent.FutureTask<String>(myCallable);
        java.util.concurrent.FutureTask<String> futureTask1 = new java.util.concurrent.FutureTask<>(myCallable1);

        ExecutorService service = Executors.newFixedThreadPool(2);
        service.execute(futureTask);
        service.execute(futureTask1); //执行任务

        while (true) {
            try {
                if (futureTask.isDone() && futureTask1.isDone()) {
                    System.out.println("Done");
                    service.shutdown();
                    return;
                }
                if (!futureTask.isDone()) {
                    System.out.println("FutureTask1 output" + futureTask.get());

                }
                System.out.println("Waitting for FutureTask to complete");
                String s = futureTask1.get(200L, TimeUnit.MILLISECONDS);
                if (s != null) {
                    System.out.println("FutureTask2 output" + s);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {

            }

        }

    }
}
