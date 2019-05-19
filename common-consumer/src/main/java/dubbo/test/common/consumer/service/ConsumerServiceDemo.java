package dubbo.test.common.consumer.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import dubbo.test.api.DemoService;
import dubbo.test.api.DemoServiceB;
import dubbo.test.domain.Student;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
public class ConsumerServiceDemo {

    @Reference(check = false, async = true, timeout = 10000)
    private DemoService demoService;

    public void demoServiceTest() {
        Student student = new Student();
        student.setSchool("xxx");
        student.setName("XXXXXX");
        demoService.sayHello(student);
        Future<Object> future = RpcContext.getContext().getFuture();
        Thread thread1 = new Thread(() -> {
            System.out.println("thead 1 run begin ," + System.currentTimeMillis());
            Object o = null;
            try {
                o = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            System.out.println("thread 1 get result:" + o + ", " + System.currentTimeMillis());

        });


        Thread thread2 = new Thread(() -> {
            System.out.println("thead 2 run begin ," + System.currentTimeMillis());
            Object o = null;
            try {
                o = future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            System.out.println("thread 2 get result:" + o + ", " + System.currentTimeMillis());

        });

        thread1.start();
        thread2.start();

    }
}
