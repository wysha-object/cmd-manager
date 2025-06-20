package cn.com.wysha.cmd_Manager.core;

import cn.com.wysha.cmd_Manager.cmd.CmdArg;
import cn.com.wysha.cmd_Manager.cmd.CmdClass;
import cn.com.wysha.cmd_Manager.cmd.CmdMethod;
import cn.com.wysha.cmd_Manager.converter.CmdConverter;
import cn.com.wysha.cmd_Manager.converter.DefaultConverter;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

/**
 * 格式:
 * CLASS_NAME METHOD_NAME {ARGS}
 */
public class CmdRunner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CmdRunner.class);
    private static CmdRunner cmdRunner = null;
    private static String basePackageName;

    private final Map<String, Class<?>> classMap = new HashMap<>();
    private final Map<Class<?>, Map<String, Method>> methodMap = new HashMap<>();
    String[] exitCmd = {"EXIT", "QUIT", "STOP"};
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private String currentPackageName = "";

    private CmdRunner(InputStream inputStream, OutputStream outputStream) {
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = new BufferedWriter(new OutputStreamWriter(outputStream));
    }

    public static void setBasePackageName(String packageName) {
        CmdRunner.basePackageName = packageName;
    }

    public static void start(InputStream inputStream, OutputStream outputStream){
        if (cmdRunner != null) throw new RuntimeException();

        cmdRunner = new CmdRunner(inputStream, outputStream);
        new Thread(cmdRunner).start();
    }

    @Override
    public void run() {
        if (CmdRunner.basePackageName == null) throw new IllegalArgumentException("Unknown basePackageName !");
        ClassInfoList classInfoList = new ClassGraph().enableAllInfo().acceptPackages(basePackageName).scan().getClassesWithAnnotation(CmdClass.class);
        List<Class<?>> list = classInfoList.loadClasses();
        for (Class<?> c : list) {
            CmdClass cmdClass = c.getAnnotation(CmdClass.class);

            String name;
            if (cmdClass.className() == null || cmdClass.className().isBlank()) {
                name = c.getName();
            } else {
                name = c.getPackageName() + '.' + cmdClass.className().trim();
            }
            name = name.toUpperCase();

            Map<String, Method> map = new HashMap<>();
            classMap.put(name, c);
            methodMap.put(c, map);

            for (Method method : c.getMethods()) {
                if (method.isAnnotationPresent(CmdMethod.class)) {
                    CmdMethod cmdMethod = method.getAnnotation(CmdMethod.class);
                    String methodName;
                    if (cmdMethod.methodName() == null || cmdMethod.methodName().isBlank()) {
                        methodName = method.getName();
                    } else {
                        methodName = cmdMethod.methodName().trim();
                    }
                    methodName = methodName.toUpperCase();

                    map.put(methodName, method);
                }
            }
        }

        try {
            writer.write("STARTED\n");

            logger.info("CMD SERVICE STARTED");

            writer.flush();

            while (true) {
                nextCmd();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void nextCmd() throws IOException {
        writer.write(String.format("[%s]\t:\t", currentPackageName));
        writer.flush();
        try {
            String string = reader.readLine();
            string = string.trim();
            String[] args = parseArgs(string);

            if (args.length < 1) {
                throw new RuntimeException("Unknown cmd");
            }
            args[0] = args[0].toUpperCase();

            for (String s : exitCmd) {
                if (args[0].equals(s)) {
                    logger.info("EXIT");
                    System.exit(0);
                }
            }

            if (args[0].equals("LS")) {
                String path = getPath(basePackageName, currentPackageName);
                File file = new File(ClassLoader.getSystemClassLoader().getResource(path).getFile());
                for (File f : file.listFiles()) {
                    if (f.isFile()) {
                        String fileName = f.getName();
                        if (fileName.toUpperCase().endsWith(".CLASS")) {
                            fileName = fileName.substring(0, fileName.length() - 6);
                            Class<?> c = Class.forName(getName(getName(basePackageName, currentPackageName), fileName));
                            if (c.isAnnotationPresent(CmdClass.class)) {
                                writer.write("[CLASS]" + '\t' + f.getName() + '\n');
                            }
                        }
                    } else {
                        writer.write("[PACKAGE]" + '\t' + f.getName() + '\n');
                    }
                }
                return;
            }

            if (args.length < 2) {
                throw new RuntimeException("Unknown cmd");
            }
            args[1] = args[1].toUpperCase();

            if (args[0].equals("CD")) {
                String tmp = args[1];
                if (tmp.startsWith("./") || tmp.startsWith(".\\")) {
                    tmp = getPath(currentPackageName, tmp.substring(2));
                }

                URL url = ClassLoader.getSystemClassLoader().getResource(getPath(basePackageName, tmp));
                if (url == null) {
                    throw new RuntimeException("Unknown package");
                }

                currentPackageName = tmp;
                return;
            }

            Class<?> c = getClassWithName(args[0]);

            if (args[1].equals("-H")) {
                CmdClass cmdClass = c.getAnnotation(CmdClass.class);
                String className = cmdClass.className();
                className = className.isEmpty() ? c.getSimpleName() : className;
                writer.write(String.format("CLASS\tHELP\t[%s]\t%s\n", className, cmdClass.help()));
                methodMap.get(c).forEach((k, v) -> {
                    try {
                        getHelp(v);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return;
            }

            Method method = getMethodWithName(c, args[1]);

            if (args.length > 2 && args[2].equals("-h")) {
                getHelp(method);
                return;
            }

            runMethod(method, args);
        } catch (Exception e) {
            logger.warn("{}", e.toString());
            writer.write(e.toString() + '\n');
            writer.write("Run failed\n");
            writer.write("Please check and repeat\n");
        }finally {
            writer.flush();
        }
    }

    private String getPath(String parent, String name) {
        return getName(parent, name).replace('.', '/');
    }

    private String getName(String parent, String name) {
        String rs = parent;
        if (!name.isEmpty()) {
            rs += (rs.isEmpty() ? "" : ".") + name;
        }
        return rs;
    }

    private Class<?> getClassWithName(String className) {
        String packageName = getName(basePackageName, currentPackageName).toUpperCase();
        Class<?> rs = classMap.get(getName(packageName, className));
        if (rs == null) throw new RuntimeException("Unknown class");
        return rs;
    }

    private Method getMethodWithName(Class<?> c, String methodName) {
        Method rs = methodMap.get(c).get(methodName);
        if (rs == null) throw new RuntimeException("Unknown method");
        return rs;
    }

    private void runMethod(Method method, String[] args) throws Exception {
        int count = method.getParameterCount();
        if ((args.length - 2) < count)
            throw new IllegalArgumentException("Arg count less than method's parameter count");
        Object[] parameters = new Object[count];

        CmdMethod cmdMethod = method.getAnnotation(CmdMethod.class);
        switch (cmdMethod.invokeMode()) {
            case ARGS -> {
                Parameter[] parameterAnnotations = method.getParameters();
                for (int i = 0; i < count; i++) {
                    String arg = args[i + 2];
                    Parameter parameter = parameterAnnotations[i];

                    Class<? extends CmdConverter> c = DefaultConverter.class;
                    if (parameter.isAnnotationPresent(CmdArg.class)) {
                        CmdArg cmdArg = parameter.getAnnotation(CmdArg.class);

                        c = cmdArg.converter();
                    }

                    CmdConverter converter = c.getDeclaredConstructor().newInstance();
                    parameters[i] = converter.stringToObj(arg);
                }
            }
            case MAP -> {
                Map<String, String> map = new HashMap<>();
                parameters[0] = map;
                for (int i = 2; i < args.length; i++) {
                    String key = args[i];
                    if (!key.startsWith("-")) throw new RuntimeException("Unknown key");

                    key = key.substring(1);

                    i++;
                    String value = i < args.length ? args[i] : "";

                    map.put(key, value);
                }
            }
            case LIST -> {
                List<String> list = new ArrayList<>();
                parameters[0] = list;
                list.addAll(Arrays.asList(args).subList(2, args.length));
            }
            default -> throw new RuntimeException("Unknown INVOKE_MODE");
        }

        logger.info("INVOKE METHOD {} WITH {}", method.getName(), Arrays.toString(args));
        Object rs = method.invoke(null, parameters);
        System.out.println(rs.toString());
    }

    private void getHelp(Method method) throws IOException {
        CmdMethod cmdMethod = method.getAnnotation(CmdMethod.class);
        String methodName = cmdMethod.methodName();
        methodName = methodName.isEmpty() ? method.getName() : methodName;
        writer.write(String.format("METHOD\tHELP\t[%s]\t%s\n", methodName, cmdMethod.help()));

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            String help = "";
            if (parameter.isAnnotationPresent(CmdArg.class)) {
                CmdArg cmdArg = parameter.getAnnotation(CmdArg.class);
                help = cmdArg.help();
            }
            writer.write(String.format("PARAMETER\tHELP\t[%s]\t[CLASS\t=\t%s]\t%s\n", parameter.getName(), parameter.getType().getName(), help));
        }
    }

    private String[] parseArgs(String cmd) {
        String[] strings = cmd.split(" ");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < strings.length; i++) {
            String s = strings[i];
            if (s.startsWith("\"")) {
                boolean isString = false;
                StringBuilder stringBuilder = new StringBuilder();
                int j;
                for (j = i; j < strings.length; j++) {
                    String string = strings[j];
                    if (j > i) {
                        stringBuilder.append(' ');
                    }
                    stringBuilder.append(string);
                    if (string.endsWith("\"") && !string.endsWith("\\\"") && ((j > i) || (string.length() >= 2))) {
                        isString = true;
                        break;
                    }
                }
                if (isString) {
                    s = stringBuilder.substring(1, stringBuilder.length() - 1);
                    i = j;
                }
            }
            list.add(s);
        }

        String[] rs = list.toArray(String[]::new);
        for (int i = 0; i < rs.length; i++) {
            rs[i] = StringEscapeUtils.unescapeJava(rs[i]);
        }
        return rs;
    }
}
