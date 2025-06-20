package cn.com.wysha.cmd_Manager.converter.number;

import cn.com.wysha.cmd_Manager.converter.CmdConverter;

public class ShortConverter implements CmdConverter<Short> {

    @Override
    public Short stringToObj(String s) {
        return Short.parseShort(s);
    }
}
