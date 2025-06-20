package cn.com.wysha.cmd_Manager.converter.number;

import cn.com.wysha.cmd_Manager.converter.CmdConverter;

public class ByteConverter implements CmdConverter<Byte> {

    @Override
    public Byte stringToObj(String s) {
        return Byte.parseByte(s);
    }
}
