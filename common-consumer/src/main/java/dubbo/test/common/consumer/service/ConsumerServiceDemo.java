package dubbo.test.common.consumer.service;

import com.alibaba.dubbo.config.annotation.Reference;
import dubbo.test.api.DemoService;
import dubbo.test.api.DemoServiceB;
import dubbo.test.domain.Student;
import org.springframework.stereotype.Component;

@Component
public class ConsumerServiceDemo {

    @Reference(check = false, interfaceName = "demoService")
    private DemoService demoService;

    public void demoServiceTest() {
        Student student = new Student();
        student.setSchool("xxx");
        student.setName("XXXXXX");

        System.out.println("from remoting: " + demoService.sayHello(student));

    }
}
