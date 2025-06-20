package cn.com.wysha.cmd_Manager.converter;

public class DefaultConverter implements CmdConverter<Object> {

    @Override
    public Object stringToObj(String s) {
        return s;
    }
}
