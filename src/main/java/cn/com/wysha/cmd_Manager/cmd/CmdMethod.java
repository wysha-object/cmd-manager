package cn.com.wysha.cmd_Manager.cmd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdMethod {
    String methodName() default "";

    String help() default "";

    InvokeMode invokeMode() default InvokeMode.ARGS;
}
