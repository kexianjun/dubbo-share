package dubbo.test.common.provider;

import com.alibaba.dubbo.config.annotation.Service;
import dubbo.test.api.DemoServiceB;

@Service
public class DemoServiceBImpl implements DemoServiceB {
    @Override
    public String demoServiceBTest(String sayHello) {
        System.out.println("from demo service B" + sayHello);
        return "from demo service B" + sayHello;
    }
}
