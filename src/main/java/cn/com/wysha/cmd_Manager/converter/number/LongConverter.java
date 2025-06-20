package cn.com.wysha.cmd_Manager.converter.number;

import cn.com.wysha.cmd_Manager.converter.CmdConverter;

public class LongConverter implements CmdConverter<Long> {

    @Override
    public Long stringToObj(String s) {
        return Long.parseLong(s);
    }
}
