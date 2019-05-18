package dubbo.test.common.provider;

import com.alibaba.dubbo.config.annotation.Service;
import dubbo.test.api.DemoService;
import dubbo.test.domain.Student;

@Service(version = "1.0")
public class DemoServiceImpl implements DemoService {
    public String sayHello(Student student) {
        System.out.println("hello from :" + student);
        return "hello from :" + student;
    }
}
