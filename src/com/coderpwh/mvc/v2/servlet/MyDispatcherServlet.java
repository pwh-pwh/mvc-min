package com.coderpwh.mvc.v2.servlet;

import com.coderpwh.mvc.v1.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author coderpwh
 * @date 2022/5/2 10:43 AM
 */
public class MyDispatcherServlet extends HttpServlet {
    //保存配置文件的内容
    private Properties properties = new Properties();
    //保存扫描的类名
    private List<String> classNames = new ArrayList<>();
    //ioc容器
    private Map<String, Object> ioc = new HashMap<>();
    //保存url到方法到映射
//    private Map<String, Method> handlerMapping = new HashMap<>();
    private List<Handler> hdMapping = new ArrayList<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        Handler handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("404 not found");
        }
        Method method = handler.method;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] paramValues = new Object[parameterTypes.length];
        Map<String, String[]> parameterMap = req.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!handler.paramIndexMapping.containsKey(entry.getKey())) {
                continue;
            }
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "").replaceAll(".\\s", ",");
            Integer index = handler.paramIndexMapping.get(entry.getKey());
            paramValues[index] = convert(parameterTypes[index], value);

        }

        Integer reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
        Integer respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramValues[reqIndex] = req;
        paramValues[respIndex] = resp;
        method.invoke(handler.object, paramValues);
    }

    private void doLoadConfig(String location) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(location)) {
            properties.load(is);
        } catch (Exception e) {

        }
    }

    private void doInstance() {
        try {
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className);
                String beanName = null;
                if (aClass.isAnnotationPresent(MyService.class)) {
                    beanName = aClass.getAnnotation(MyService.class).value();
                    if ("".equals(beanName)) {
                        beanName = className;
                    }
                    Object o = aClass.newInstance();
                    ioc.put(beanName, o);
                    for (Class<?> anInterface : aClass.getInterfaces()) {
                        ioc.put(anInterface.getName(), o);
                    }
                } else if (aClass.isAnnotationPresent(MyController.class)) {
                    beanName = aClass.getAnnotation(MyController.class).value();
                    if ("".equals(beanName)) {
                        beanName = className;
                    }
                    Object o = aClass.newInstance();
                    ioc.put(beanName, o);
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutowired() {
        try {
            for (Object obj : ioc.values()) {
                Class<?> aClass = obj.getClass();
                Field[] declaredFields = aClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (declaredField.isAnnotationPresent(MyAutowired.class)) {
                        MyAutowired myAutowired = declaredField.getAnnotation(MyAutowired.class);
                        String beanName = myAutowired.value();
                        if ("".equals(beanName)) {
                            beanName = declaredField.getType().getName();
                        }
                        declaredField.setAccessible(true);
                        declaredField.set(obj, ioc.get(beanName));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void initHandlerMapping() {
        for (Object obj : ioc.values()) {
            Class<?> aClass = obj.getClass();
            //如果存在controller
            if (aClass.isAnnotationPresent(MyController.class)) {
                String baseUrl = "";
                if (aClass.isAnnotationPresent(MyRequestMapping.class)) {
                    baseUrl = aClass.getAnnotation(MyRequestMapping.class).value();
                }
                Method[] methods = aClass.getMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(MyRequestMapping.class)) {
                        String mp = method.getAnnotation(MyRequestMapping.class).value();
                        String url = ("/" + baseUrl + mp).replaceAll("/+", "/");
                        Pattern pattern = Pattern.compile(url);
                        hdMapping.add(new Handler(obj, method, pattern));
//                        handlerMapping.put(url, method);
                    }
                }
            }
        }
    }

    @Override
    public void init(ServletConfig config) {
        //加载配置文件
        doLoadConfig(config.getInitParameter("configLocation"));
        //扫描所有类
        doScanner(properties.getProperty("scanPackages"));
        //实例化对象
        doInstance();
        //完成依赖注入
        doAutowired();
        //初始化hanlderMapping
        initHandlerMapping();
        System.out.println("success init");
        logInfo();
    }


    private void logInfo() {
        System.out.println("log beanName list");
        classNames.forEach(System.out::println);
        System.out.println("log ioc info");
        ioc.entrySet().forEach(System.out::println);
        System.out.println("handlerMapping info");
        hdMapping.forEach(System.out::println);
    }

    private Handler getHandler(HttpServletRequest req) {
        if (hdMapping.isEmpty()) {
            return null;
        }
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        uri = uri.replace(contextPath, "").replaceAll("/+", "/");
        for (Handler handler : hdMapping) {
            Matcher matcher = handler.pattern.matcher(uri);
            if (!matcher.matches()) {
                continue;
            }
            return handler;
        }

        return null;
    }

    private Object convert(Class<?> type, String value) {
        if (type == Integer.class) {
            return Integer.valueOf(value);
        }
        if (type == String.class) {
            return value;
        }
        throw new RuntimeException("不支持的类型转换");
    }

    private void doScanner(String scanPackage) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            //是目录
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + file.getName().replace(".class", "");
//                mapping.put(className,null);
                classNames.add(className);
            }
        }


    }

    /**
     * Handler记录Controller中的RequestMapping和Method的对应关系
     *
     * @author Tom
     * 内部类
     */
    private class Handler {
        //保存方法对应实例
        private Object object;
        //保存对应方法
        private Method method;
        //pattern
        private Pattern pattern;

        private Map<String, Integer> paramIndexMapping;    //参数顺序

        public Handler(Object object, Method method, Pattern pattern) {
            this.object = object;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) annotation).value();
                        if (!paramName.trim().equals("")) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == HttpServletRequest.class || parameterTypes[i] == HttpServletResponse.class) {
                    paramIndexMapping.put(parameterTypes[i].getName(), i);
                }
            }

        }
    }

}
