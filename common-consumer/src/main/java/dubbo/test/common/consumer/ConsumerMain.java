package dubbo.test.common.consumer;

import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import dubbo.test.common.consumer.service.ConsumerServiceDemo;
import dubbo.test.common.consumer.service.GenericServiceCallDemo;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

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
        serviceCallDemo.testMethod(map, result -> {
            System.out.println("async result:" + result);
            return result;
        });
        System.out.println("call return");
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
