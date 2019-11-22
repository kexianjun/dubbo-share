package dubbo.test.common.consumer.service;

public class CallbackFunction {
    public void callback(Object result, Object param, @CallBackParam(name = "userParam") String userParam) {
        System.out.println("result:" + result);
        System.out.println("param:" + param);
        System.out.println("userParam:" + userParam);
    }

    public void onThrow(Object exception, Object param, @CallBackParam(name = "onThrow") String userParam) {
        System.out.println("exception:" + exception);
        System.out.println("param:" + param);
        System.out.println("userParam:" + userParam);
    }
}
