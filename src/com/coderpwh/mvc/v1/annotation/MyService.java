package com.coderpwh.mvc.v1.annotation;

import java.lang.annotation.*;

/**
 * @author coderpwh
 * @date 2022/5/2 10:59 AM
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyService {
    String value() default "";
}
