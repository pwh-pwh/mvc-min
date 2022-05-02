package com.coderpwh.mvc.v1.annotation;

import java.lang.annotation.*;

/**
 * @author coderpwh
 * @date 2022/5/2 11:03 AM
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyController {
    String value() default "";
}
