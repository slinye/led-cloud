package com.led.common.annotation;

import java.lang.annotation.*;

/** 审计日志注解，标注在需要记录操作日志的Controller方法上 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** 操作模块 */
    String module() default "";
    /** 操作动作 */
    String action() default "";
    /** 操作描述 */
    String description() default "";
}
