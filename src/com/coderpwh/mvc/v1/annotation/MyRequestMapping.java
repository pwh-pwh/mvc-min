package com.coderpwh.mvc.v1.annotation;

import java.lang.annotation.*;

/**
 * @author coderpwh
 * @date 2022/5/2 11:04 AM
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestMapping {
    String value() default "";
}
