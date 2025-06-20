package cn.com.wysha.cmd_Manager.converter.number;

import cn.com.wysha.cmd_Manager.converter.CmdConverter;

public class IntegerConverter implements CmdConverter<Integer> {

    @Override
    public Integer stringToObj(String s) {
        return Integer.parseInt(s);
    }
}
