package dubbo.test.common.provider;

import com.alibaba.dubbo.config.annotation.Service;
import dubbo.test.api.DemoService;
import dubbo.test.domain.Student;

@Service(interfaceName = "dubbo.test.api.DemoService")
public class DemoServiceImpl implements DemoService {
    public String sayHello(Student student) {
        System.out.println("hello from :" + student);
        return "hello from :" + student;
    }
}
