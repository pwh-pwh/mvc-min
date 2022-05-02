package com.coderpwh.mvc.v1.servlet;

import com.coderpwh.mvc.v1.annotation.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * @author coderpwh
 * @date 2022/5/2 10:43 AM
 */
public class MyDispatcherServlet extends HttpServlet {
    private Map<String,Object> mapping = new HashMap<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500 Exception "+ Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath,"").replaceAll("/+","/");
        if (!this.mapping.containsKey(uri)) {
            resp.getWriter().write("404 not found");
            return;
        }
        Method method = (Method) mapping.get(uri);
        Map<String, String[]> params = req.getParameterMap();
        Parameter[] parameters = method.getParameters();
        if (parameters.length==2) {
            method.invoke(mapping.get(method.getDeclaringClass().getName()),new Object[]{
                    req,resp
            });
        }else {
            Object[] objects = new Object[parameters.length];
            objects[0] = req;
            objects[1] = resp;
            for (int i = 2; i < parameters.length; i++) {
                String name = parameters[i].getAnnotation(MyRequestParam.class).value();
                Class<?> type = parameters[i].getType();
                String rp = params.get(name)[0];
                if (type.equals(String.class)) {
                    objects[i] = rp;
                } else if (type.equals(Integer.class)) {
                    objects[i] = Integer.parseInt(rp);
                } else {
                    throw new RuntimeException("不支持类型的参数");
                }
            }
            method.invoke(mapping.get(method.getDeclaringClass().getName()),objects);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        String configLocation = config.getInitParameter("configLocation");
        InputStream is = getClass().getClassLoader().getResourceAsStream(configLocation);
        Properties properties = new Properties();
        try {
            properties.load(is);
            String scanPackages = properties.getProperty("scanPackages");
            //扫描所有类
            doScanner(scanPackages);
            ArrayList<String> mappingList = new ArrayList<>(mapping.keySet());
            for (String className : mappingList) {
                if (!className.contains(".")){
                    continue;
                }
                Class<?> aClass = Class.forName(className);
                //存在自定义controller注解
                if (aClass.isAnnotationPresent(MyController.class)) {
                    mapping.put(className,aClass.newInstance());
                    String baseUrl = "";
                    if (aClass.isAnnotationPresent(MyRequestMapping.class)) {
                        MyRequestMapping requestMapping = aClass.getAnnotation(MyRequestMapping.class);
                        baseUrl = requestMapping.value();
                    }
                    Method[] methods = aClass.getMethods();
                    for (Method method : methods) {
                        if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                            continue;
                        }
                        MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                        String url = (baseUrl + requestMapping.value()).replaceAll("/+","/");
                        mapping.put(url,method);
                        System.out.println("mapped "+url+" ,"+method);
                    }
                } else if (aClass.isAnnotationPresent(MyService.class)) {
                    MyService myService = aClass.getAnnotation(MyService.class);
                    String serviceName = myService.value();
                    if (serviceName.equals("")) {
                        serviceName = aClass.getName();
                    }
                    Object o = aClass.newInstance();
                    mapping.put(serviceName,o);
                    //给每个接口都映射
                    for (Class<?> anInterface : aClass.getInterfaces()) {
                        mapping.put(anInterface.getName(),o);
                    }
                } else {
                    continue;
                }

            }

            //对mapping中的类进行注入
            for (Object value : mapping.values()) {
                if (value==null) {
                    continue;
                }
                Class<?> aClass = value.getClass();
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(MyAutowired.class)) {
                        continue;
                    }
                    String beanName = field.getAnnotation(MyAutowired.class).value();
                    if ("".equals(beanName)) {
                        beanName = field.getType().getName();
                    }
                    field.setAccessible(true);
                    field.set(value,mapping.get(beanName));
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }  finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("success init servlet");
            System.out.println("mapping result :");
            for (Map.Entry<String, Object> entry : mapping.entrySet()) {
                System.out.println(entry.getKey()+":"+entry.getValue());
            }
        }

    }

    private void doScanner(String scanPackage) {
        URL url = Thread.currentThread().getContextClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            //是目录
            if (file.isDirectory()) {
                doScanner(scanPackage+"."+file.getName());
            }else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." +file.getName().replace(".class","");
                mapping.put(className,null);
            }
        }


    }

}
