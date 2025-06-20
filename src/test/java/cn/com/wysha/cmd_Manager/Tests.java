package cn.com.wysha.cmd_Manager;

import cn.com.wysha.cmd_Manager.cmd.CmdClass;
import cn.com.wysha.cmd_Manager.cmd.CmdMethod;
import cn.com.wysha.cmd_Manager.core.CmdRunner;

@CmdClass(help = "h0")
public class Tests {
    public static void main(String[] args) {
        CmdRunner.setBasePackageName("cn.com.wysha.cmd_Manager");
        CmdRunner.start(System.in, System.out);
    }

    @CmdMethod(help = "h1")
    public static String test(String s1,String s2) {
        return s1 + s2;
    }
}
