package cn.com.wysha.cmd_Manager.cmd;

import cn.com.wysha.cmd_Manager.converter.CmdConverter;
import cn.com.wysha.cmd_Manager.converter.DefaultConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CmdArg {
    String help() default "";

    Class<? extends CmdConverter> converter() default DefaultConverter.class;
}
