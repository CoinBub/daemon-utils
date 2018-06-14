package tech.coinbub.daemon.proxy;

import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.ReflectionUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import tech.coinbub.daemon.proxy.annotation.Convert;

public class ProxyUtil {
    public static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final IJsonRpcClient client) {
        return createClientProxy(classLoader, proxyInterface, client, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static <T> T createClientProxy(ClassLoader classLoader, Class<T> proxyInterface, final IJsonRpcClient client, final Map<String, String> extraHeaders) {
        System.out.println("### Create proxy");
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{proxyInterface}, (Object proxy, Method method, Object[] args) -> {
            if (isDeclaringClassAnObject(method)) {
                System.out.println("### Class is an object");
                return proxyObjectMethods(method, proxy, args);
            }

            // Convert parameters
            final Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                final Parameter param = parameters[i];
                if (param.isAnnotationPresent(Convert.class)) {
                    System.out.println("### Convert parameter " + param.getName());
                    final Convert convert = param.getAnnotation(Convert.class);
                    final Converter converter = convert.value().newInstance();
                    args[i] = converter.marshal(args[i]);
                }
            }
            final Object arguments = ReflectionUtil.parseArguments(method, args);
            final String methodName = getMethodName(method);
            if (method.isAnnotationPresent(Convert.class)) {
                final Convert convert = method.getAnnotation(Convert.class);
                final Converter converter = convert.value().newInstance();
                return converter.unmarshal(client.invoke(methodName, arguments, Object.class, extraHeaders));
            }
            return client.invoke(methodName, arguments, method.getGenericReturnType(), extraHeaders);
        });
    }

    private static boolean isDeclaringClassAnObject(Method method) {
        return method.getDeclaringClass() == Object.class;
    }
	
    private static Object proxyObjectMethods(Method method, Object proxyObject, Object[] args) {
        String name = method.getName();
        if (name.equals("toString")) {
            return proxyObject.getClass().getName() + "@" + System.identityHashCode(proxyObject);
        }
        if (name.equals("hashCode")) {
            return System.identityHashCode(proxyObject);
        }
        if (name.equals("equals")) {
            return proxyObject == args[0];
        }
        throw new RuntimeException(method.getName() + " is not a member of java.lang.Object");
    }
	
    private static String getMethodName(Method method) {
        final JsonRpcMethod jsonRpcMethod = ReflectionUtil.getAnnotation(method, JsonRpcMethod.class);
        if (jsonRpcMethod == null) {
            return method.getName();
        }
        return jsonRpcMethod.value();
    }
}
