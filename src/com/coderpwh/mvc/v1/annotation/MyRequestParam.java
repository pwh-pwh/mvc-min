package com.coderpwh.mvc.v1.annotation;

import java.lang.annotation.*;

/**
 * @author coderpwh
 * @date 2022/5/2 11:05 AM
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRequestParam {
    String value() default "";
}
