package com.coderpwh.mvc.v1.annotation;

import java.lang.annotation.*;

/**
 * @author coderpwh
 * @date 2022/5/2 11:02 AM
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAutowired {
    String value() default "";
}
