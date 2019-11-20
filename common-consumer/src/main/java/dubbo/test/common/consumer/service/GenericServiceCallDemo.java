package dubbo.test.common.consumer.service;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@Component
public class GenericServiceCallDemo {

    @Resource
    private RegistryConfig registry;
    @Resource
    private ApplicationConfig application;

    public void testMethod(Map<String, Object> map, Function<String, String> function) {
        ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setRegistry(registry);
        referenceConfig.setApplication(application);
        referenceConfig.setInterface("dubbo.test.api.DemoService");
        referenceConfig.setGeneric(true);
        referenceConfig.setFilter("-future");
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayHello");
        methodConfig.setOnreturn(function);
        methodConfig.setOnreturnMethod("apply");
        referenceConfig.setAsync(true);
        referenceConfig.setMethods(Collections.singletonList(methodConfig));
        GenericService genericService = referenceConfig.get();

        genericService.$invoke("sayHello", new String[]{"dubbo.test.domain.Student"}, new Object[]{map});
    }
}
