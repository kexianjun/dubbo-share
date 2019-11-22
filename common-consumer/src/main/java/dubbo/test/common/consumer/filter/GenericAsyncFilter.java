package dubbo.test.common.consumer.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.model.ApplicationModel;
import com.alibaba.dubbo.config.model.ConsumerModel;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.StaticContext;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import com.alibaba.dubbo.rpc.protocol.dubbo.filter.FutureFilter;
import com.alibaba.dubbo.rpc.support.RpcUtils;
import dubbo.test.common.consumer.service.CallBackParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Future;

@Activate(group = Constants.CONSUMER, order = 100)
public class GenericAsyncFilter implements Filter {
    protected static final Logger logger = LoggerFactory.getLogger(FutureFilter.class);

    @Override
    public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {
        final boolean isAsync = RpcUtils.isAsync(invoker.getUrl(), invocation);

        fireInvokeCallback(invoker, invocation);
        // need to configure if there's return value before the invocation in order to help invoker to judge if it's
        // necessary to return future.
        Result result = invoker.invoke(invocation);
        if (isAsync) {
            asyncCallback(invoker, invocation);
        } else {
            syncCallback(invoker, invocation, result);
        }
        return result;
    }

    private void syncCallback(final Invoker<?> invoker, final Invocation invocation, final Result result) {
        if (result.hasException()) {
            fireThrowCallback(invoker, invocation, result.getException());
        } else {
            fireReturnCallback(invoker, invocation, result.getValue());
        }
    }

    private void asyncCallback(final Invoker<?> invoker, final Invocation invocation) {
        RpcContext context = RpcContext.getContext();
        Future<?> f = context.getFuture();
        if (f instanceof FutureAdapter) {
            ResponseFuture future = ((FutureAdapter<?>) f).getFuture();
            future.setCallback(new ResponseCallback() {
                @Override
                public void done(Object rpcResult) {
                    RpcContext rpcContext = RpcContext.getContext();
                    Map<String, Object> valueMap = context.get();
                    if (null != valueMap && !valueMap.isEmpty()) {
                        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                            rpcContext.set(entry.getKey(), entry.getValue());
                        }
                    }
                    try {
                        if (rpcResult == null) {
                            logger.error(new IllegalStateException("invalid result value : null, expected " + Result.class.getName()));
                            return;
                        }
                        ///must be rpcResult
                        if (!(rpcResult instanceof Result)) {
                            logger.error(new IllegalStateException("invalid result type :" + rpcResult.getClass() + ", expected " + Result.class.getName()));
                            return;
                        }
                        Result result = (Result) rpcResult;
                        if (result.hasException()) {
                            fireThrowCallback(invoker, invocation, result.getException());
                        } else {
                            fireReturnCallback(invoker, invocation, result.getValue());
                        }
                    } finally {
                        RpcContext.removeContext();
                    }
                }

                @Override
                public void caught(Throwable exception) {
                    RpcContext rpcContext = RpcContext.getContext();
                    Map<String, Object> valueMap = context.get();
                    if (null != valueMap && !valueMap.isEmpty()) {
                        for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                            rpcContext.set(entry.getKey(), entry.getValue());
                        }
                    }
                    try{
                        fireThrowCallback(invoker, invocation, exception);
                    }finally {
                        RpcContext.removeContext();
                    }
                }
            });
        }
    }

    private void fireInvokeCallback(final Invoker<?> invoker, final Invocation invocation) {
        final Method onInvokeMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_INVOKE_METHOD_KEY));
        final Object onInvokeInst = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), invocation.getMethodName(), Constants.ON_INVOKE_INSTANCE_KEY));

        if (onInvokeMethod == null && onInvokeInst == null) {
            return;
        }
        if (onInvokeMethod == null || onInvokeInst == null) {
            throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onreturn callback config , but no such " + (onInvokeMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
        }
        if (!onInvokeMethod.isAccessible()) {
            onInvokeMethod.setAccessible(true);
        }

        Object[] params = invocation.getArguments();
        try {
            onInvokeMethod.invoke(onInvokeInst, params);
        } catch (InvocationTargetException e) {
            fireThrowCallback(invoker, invocation, e.getTargetException());
        } catch (Throwable e) {
            fireThrowCallback(invoker, invocation, e);
        }
    }

    private void fireReturnCallback(final Invoker<?> invoker, final Invocation invocation, final Object result) {
        AsyncMethodInfo asyncMethodInfo = getAsyncMethodInfo(invoker, invocation);
        if (null == asyncMethodInfo) {
            return;
        }
        final Method onReturnMethod = asyncMethodInfo.getOnreturnMethod();
        final Object onReturnInst = asyncMethodInfo.getOnreturnInstance();

        //not set onreturn callback
        if (onReturnMethod == null && onReturnInst == null) {
            return;
        }

        if (onReturnMethod == null || onReturnInst == null) {
            throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onreturn callback config , but no such " + (onReturnMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
        }
        if (!onReturnMethod.isAccessible()) {
            onReturnMethod.setAccessible(true);
        }

        Object[] args = invocation.getArguments();
        Object[] params;
        Class<?>[] rParaTypes = onReturnMethod.getParameterTypes();
        boolean generic = invoker.getUrl().getParameter(Constants.GENERIC_KEY, false);
            if (rParaTypes.length > 1) {
                if (rParaTypes.length == 2 && rParaTypes[1].isAssignableFrom(Object[].class)) {
                    params = new Object[2];
                    params[0] = result;
                    params[1] = args;
                } else {
                    if (!generic) {
                        params = new Object[args.length + 1];
                        params[0] = result;
                        System.arraycopy(args, 0, params, 1, args.length);
                    } else {
                        params = new Object[rParaTypes.length];
                        params[0] = result;
                        params[1] = args;
                        int index = 2;
                        Annotation[][] parameterAnnotations = onReturnMethod.getParameterAnnotations();
                        for (Annotation[] annotations : parameterAnnotations) {
                            for (Annotation annotation : annotations) {
                                if (annotation instanceof CallBackParam) {
                                    CallBackParam callBackParamAnnotation = (CallBackParam) annotation;
                                    String name = callBackParamAnnotation.name();
                                    if (StringUtils.isNotEmpty(name)) {
                                        RpcContext context = RpcContext.getContext();
                                        Object userParam = context.get(name);
                                        params[index++] = userParam;
                                    }
                                }
                            }
                        }

                    }
                }
            } else {
                params = new Object[]{result};
            }

        try {
            onReturnMethod.invoke(onReturnInst, params);
        } catch (InvocationTargetException e) {
            fireThrowCallback(invoker, invocation, e.getTargetException());
        } catch (Throwable e) {
            fireThrowCallback(invoker, invocation, e);
        }
    }

    private void fireThrowCallback(final Invoker<?> invoker, final Invocation invocation, final Throwable exception) {
        AsyncMethodInfo asyncMethodInfo = getAsyncMethodInfo(invoker, invocation);
        if (null == asyncMethodInfo) {
            return;
        }
        Object onthrowInst = asyncMethodInfo.getOnthrowInstance();
        Method onthrowMethod = asyncMethodInfo.getOnthrowMethod();
        //onthrow callback not configured
        if (onthrowInst == null && onthrowMethod == null) {
            return;
        }
        if (onthrowMethod == null || onthrowInst == null) {
            throw new IllegalStateException("service:" + invoker.getUrl().getServiceKey() + " has a onthrow callback config , but no such " + (onthrowMethod == null ? "method" : "instance") + " found. url:" + invoker.getUrl());
        }
        if (!onthrowMethod.isAccessible()) {
            onthrowMethod.setAccessible(true);
        }
        Class<?>[] rParaTypes = onthrowMethod.getParameterTypes();
        boolean generic = invoker.getUrl().getParameter(Constants.GENERIC_KEY, false);
        if (rParaTypes[0].isAssignableFrom(exception.getClass())) {
            try {
                Object[] args = invocation.getArguments();
                Object[] params;

                if (rParaTypes.length > 1) {
                    if (rParaTypes.length == 2 && rParaTypes[1].isAssignableFrom(Object[].class)) {
                        params = new Object[2];
                        params[0] = exception;
                        params[1] = args;
                    } else {
                        if (!generic) {
                            params = new Object[args.length + 1];
                            params[0] = exception;
                            System.arraycopy(args, 0, params, 1, args.length);
                        } else {
                            params = new Object[rParaTypes.length];
                            params[0] = exception;
                            params[1] = args;
                            int index = 2;
                            Annotation[][] parameterAnnotations = onthrowMethod.getParameterAnnotations();
                            for (Annotation[] annotations : parameterAnnotations) {
                                for (Annotation annotation : annotations) {
                                    if (annotation instanceof CallBackParam) {
                                        CallBackParam callBackParamAnnotation = (CallBackParam) annotation;
                                        String name = callBackParamAnnotation.name();
                                        if (StringUtils.isNotEmpty(name)) {
                                            RpcContext context = RpcContext.getContext();
                                            Object userParam = context.get(name);
                                            params[index++] = userParam;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    params = new Object[]{exception};
                }
                onthrowMethod.invoke(onthrowInst, params);
            } catch (Throwable e) {
                logger.error(invocation.getMethodName() + ".call back method invoke error . callback method :" + onthrowMethod + ", url:" + invoker.getUrl(), e);
            }
        } else {
            logger.error(invocation.getMethodName() + ".call back method invoke error . callback method :" + onthrowMethod + ", url:" + invoker.getUrl(), exception);
        }
    }

    private AsyncMethodInfo getAsyncMethodInfo(Invoker<?> invoker, Invocation invocation) {
        ConsumerModel consumerModel = ApplicationModel.getConsumerModel(invoker.getUrl().getParameter("interface"));

        if (null == consumerModel) {
            return null;
        }
        String methodName = invocation.getMethodName();
        if (methodName.equals("$invoke")) {
            methodName = (String) invocation.getArguments()[0];
        }
        final Method onReturnMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), methodName, Constants.ON_RETURN_METHOD_KEY));
        final Object onReturnInstance = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), methodName, Constants.ON_RETURN_INSTANCE_KEY));
        AsyncMethodInfo asyncMethodInfo = new AsyncMethodInfo();
        asyncMethodInfo.setOnreturnInstance(onReturnInstance);
        asyncMethodInfo.setOnreturnMethod(onReturnMethod);

        final Method onThrowMethod = (Method) StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), methodName, Constants.ON_THROW_METHOD_KEY));
        final Object onThrowInstance = StaticContext.getSystemContext().get(StaticContext.getKey(invoker.getUrl(), methodName, Constants.ON_THROW_INSTANCE_KEY));

        asyncMethodInfo.setOnthrowInstance(onThrowInstance);
        asyncMethodInfo.setOnthrowMethod(onThrowMethod);
        return asyncMethodInfo;
    }

    public static class AsyncMethodInfo {
        // callback instance when async-call is invoked
        private Object oninvokeInstance;

        // callback method when async-call is invoked
        private Method oninvokeMethod;

        // callback instance when async-call is returned
        private Object onreturnInstance;

        // callback method when async-call is returned
        private Method onreturnMethod;

        // callback instance when async-call has exception thrown
        private Object onthrowInstance;

        // callback method when async-call has exception thrown
        private Method onthrowMethod;

        public Object getOninvokeInstance() {
            return oninvokeInstance;
        }

        public void setOninvokeInstance(Object oninvokeInstance) {
            this.oninvokeInstance = oninvokeInstance;
        }

        public Method getOninvokeMethod() {
            return oninvokeMethod;
        }

        public void setOninvokeMethod(Method oninvokeMethod) {
            this.oninvokeMethod = oninvokeMethod;
        }

        public Object getOnreturnInstance() {
            return onreturnInstance;
        }

        public void setOnreturnInstance(Object onreturnInstance) {
            this.onreturnInstance = onreturnInstance;
        }

        public Method getOnreturnMethod() {
            return onreturnMethod;
        }

        public void setOnreturnMethod(Method onreturnMethod) {
            this.onreturnMethod = onreturnMethod;
        }

        public Object getOnthrowInstance() {
            return onthrowInstance;
        }

        public void setOnthrowInstance(Object onthrowInstance) {
            this.onthrowInstance = onthrowInstance;
        }

        public Method getOnthrowMethod() {
            return onthrowMethod;
        }

        public void setOnthrowMethod(Method onthrowMethod) {
            this.onthrowMethod = onthrowMethod;
        }
    }
}
