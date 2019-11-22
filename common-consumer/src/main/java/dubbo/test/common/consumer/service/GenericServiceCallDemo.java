package dubbo.test.common.consumer.service;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

@Component
public class GenericServiceCallDemo {

    private volatile GenericService genericService = null;

    @Resource
    private RegistryConfig registry;
    @Resource
    private ApplicationConfig application;
    private CallbackFunction callbackFunction = new CallbackFunction();

    public void testMethod(Map<String, Object> map) {
        if (genericService == null) {
            ReferenceConfig<GenericService> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setRegistry(registry);
            referenceConfig.setApplication(application);
            referenceConfig.setInterface("dubbo.test.api.DemoService");
            referenceConfig.setGeneric(true);
            referenceConfig.setFilter("-future");
            MethodConfig methodConfig = new MethodConfig();
            methodConfig.setName("sayHello");
            methodConfig.setOnreturn(callbackFunction);
            methodConfig.setOnreturnMethod("callback");
            methodConfig.setOnthrow(callbackFunction);
            methodConfig.setOnthrowMethod("onThrow");
            referenceConfig.setAsync(true);
            referenceConfig.setMethods(Collections.singletonList(methodConfig));
            genericService = referenceConfig.get();
        }
        RpcContext context = RpcContext.getContext();

        context.set("userParam", (System.currentTimeMillis() + Math.random()) + "");
        context.set("onThrow", "onThrow:" + System.currentTimeMillis());

        genericService.$invoke("sayHello", new String[]{"dubbo.test.domain.Student"}, new Object[]{map});
        RpcContext.removeContext();
    }
}
