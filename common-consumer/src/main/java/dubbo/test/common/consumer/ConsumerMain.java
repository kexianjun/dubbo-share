package dubbo.test.common.consumer;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import com.alibaba.dubbo.rpc.RpcContext;
import dubbo.test.common.consumer.service.GenericServiceCallDemo;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class ConsumerMain {
    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConsumerConfiguration.class);
        context.start();
        /*ConsumerServiceDemo serviceDemo = context.getBean(ConsumerServiceDemo.class);
        serviceDemo.demoServiceTest();
*/
        GenericServiceCallDemo serviceCallDemo = context.getBean(GenericServiceCallDemo.class);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "KEXIANJUN");
        map.put("school", "WUST");
        RpcContext.getContext().set("callback-param-1", "call first");
        serviceCallDemo.testMethod(map);
        RpcContext.removeContext();
        System.out.println("call return");

        RpcContext.getContext().set("callback-param-1", "call second");
        serviceCallDemo.testMethod(map);

        RpcContext.removeContext();

        RpcContext.getContext().set("callback-param-1", "call third");
        serviceCallDemo.testMethod(map);
        RpcContext.removeContext();
        System.in.read();
    }

    @Configuration
    @ComponentScan(value = "dubbo.test.common.consumer.service")
    @EnableDubbo(scanBasePackages = "dubbo.test.common.consumer.service")
    @PropertySource("classpath:dubbo-consumer.properties")
    static class ConsumerConfiguration {
        /*@Bean
        public RegistryConfig registryConfig() {
            RegistryConfig registryConfig = new RegistryConfig();
            registryConfig.setAddress("zookeeper://127.0.0.1:2181");
            return registryConfig;
        }*/
    }
}
