package cn.com.wysha.cmd_Manager.converter;

public interface CmdConverter<T> {
    T stringToObj(String s);
}
