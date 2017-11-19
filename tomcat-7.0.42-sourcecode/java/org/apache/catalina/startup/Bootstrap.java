/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.catalina.startup;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.catalina.Globals;
import org.apache.catalina.security.SecurityClassLoad;
import org.apache.catalina.startup.ClassLoaderFactory.Repository;
import org.apache.catalina.startup.ClassLoaderFactory.RepositoryType;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;


/**
 * Bootstrap loader for Catalina.  This application constructs a class loader
 * for use in loading the Catalina internal classes (by accumulating all of the
 * JAR files found in the "server" directory under "catalina.home"), and
 * starts the regular execution of the container.  The purpose of this
 * roundabout approach is to keep the Catalina internal classes (and any
 * other classes they depend on, such as an XML parser) out of the system
 * class path and therefore not visible to application level classes.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Id: Bootstrap.java 1428018 2013-01-02 20:41:24Z markt $
`` *
 * 启动类
 */

public final class Bootstrap {

    private static final Log log = LogFactory.getLog(Bootstrap.class);


    // ------------------------------------------------------- Static Variables


    /**
     * Daemon object used by main.
     */
    private static Bootstrap daemon = null;


    // -------------------------------------------------------------- Variables


    /**
     * Daemon reference.
     */
    private Object catalinaDaemon = null;


    protected ClassLoader commonLoader = null;
    protected ClassLoader catalinaLoader = null;
    protected ClassLoader sharedLoader = null;


    // -------------------------------------------------------- Private Methods


    private void initClassLoaders() {
        try {
            // 创建一个"公用" 类加载器 父类加载器为 null
            commonLoader = createClassLoader("common", null);
            if( commonLoader == null ) {
                // no config file, default to this loader - we might be in a 'single' env.
                commonLoader=this.getClass().getClassLoader();// 如果为 null, 则使用默认的 classloader
            }
            // 下面两个类加载的父加载器为上面的公用加载器
            // catalina.properties 中 key:server.loader=    value 为空, 所以直接返回  父类加载器
            catalinaLoader = createClassLoader("server", commonLoader);
            // 同上...返回父类加载器
            sharedLoader = createClassLoader("shared", commonLoader);
        } catch (Throwable t) {
            handleThrowable(t);
            log.error("Class loader creation threw exception", t);
            System.exit(1);
        }
    }


    private ClassLoader createClassLoader(String name, ClassLoader parent)
        throws Exception {

        // 获取 ${catalina.base}/lib,
        // ${catalina.base}/lib/*.jar,
        // ${catalina.home}/lib,
        // ${catalina.home}/lib/*.jar
        String value = CatalinaProperties.getProperty(name + ".loader");
        if ((value == null) || (value.equals("")))
            return parent;

        // Value= catalina-home/lib,
        // catalina-home/lib/*.jar,
        // catalina-home/lib,
        // catalina-home/lib/*.jar
        value = replace(value);

//      ClassLoaderFactory  中的一个静态内部类, 有2个属性, location, type, 某个位置的某种类型的文件??
        List<Repository> repositories = new ArrayList<Repository>();
      // 字符串标记类,
        StringTokenizer tokenizer = new StringTokenizer(value, ",");
        while (tokenizer.hasMoreElements()) {
            String repository = tokenizer.nextToken().trim();// 根据,分割 value,获取到每一个目录
            if (repository.length() == 0) {
                continue;
            }

            // Check for a JAR URL repository
            try {
                @SuppressWarnings("unused")
                URL url = new URL(repository);// 根据目录名称创建一个 URL
                repositories.add( // 向集合添加URL 类型的文件位置, 构造参数是"位置", 类型"URL";
                        new Repository(repository, RepositoryType.URL));
                continue;// 跳出当前循环
            } catch (MalformedURLException e) {
                // Ignore
            }

            // Local repository
            if (repository.endsWith("*.jar")) {
                repository = repository.substring
                    (0, repository.length() - "*.jar".length());
                repositories.add(
                        new Repository(repository, RepositoryType.GLOB));
            } else if (repository.endsWith(".jar")) {
                repositories.add(
                        new Repository(repository, RepositoryType.JAR));
            } else {
                repositories.add(
                        new Repository(repository, RepositoryType.DIR));
            }
        }

        // 根据给定的路径数组前去加载给定的 class 文件,
        // StandardClassLoader 继承了 java.net.URLClassLoader ,使用URLClassLoader的构造器构造类加载器.
        // 根据父类加载器是否为 null, URLClassLoader将启用不同的构造器.
        // 总之, common 类加载器没有指定父类加载器,违背双亲委派模型
        ClassLoader classLoader = ClassLoaderFactory.createClassLoader// 创建类加载器, 如果parent 是0,则
            (repositories, parent);

        // Retrieving MBean server
        MBeanServer mBeanServer = null;
        if (MBeanServerFactory.findMBeanServer(null).size() > 0) {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
        } else {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }

        // Register the server classloader
        ObjectName objectName =
            new ObjectName("Catalina:type=ServerClassLoader,name=" + name);
        // 生命周期管理.
        mBeanServer.registerMBean(classLoader, objectName);

        return classLoader;

    }

    /**
     * System property replacement in the given string.
     *
     * @param str The original string
     * @return the modified string
     */
    protected String replace(String str) {
        // Implementation is copied from ClassLoaderLogManager.replace(),
        // but added special processing for catalina.home and catalina.base.
        String result = str;
        int pos_start = str.indexOf("${");
        if (pos_start >= 0) {
            StringBuilder builder = new StringBuilder();
            int pos_end = -1;
            while (pos_start >= 0) {
                builder.append(str, pos_end + 1, pos_start);
                pos_end = str.indexOf('}', pos_start + 2);
                if (pos_end < 0) {
                    pos_end = pos_start - 1;
                    break;
                }
                String propName = str.substring(pos_start + 2, pos_end);
                String replacement;
                if (propName.length() == 0) {
                    replacement = null;
                } else if (Globals.CATALINA_HOME_PROP.equals(propName)) {
                    replacement = getCatalinaHome();
                } else if (Globals.CATALINA_BASE_PROP.equals(propName)) {
                    replacement = getCatalinaBase();
                } else {
                    replacement = System.getProperty(propName);
                }
                if (replacement != null) {
                    builder.append(replacement);
                } else {
                    builder.append(str, pos_start, pos_end + 1);
                }
                pos_start = str.indexOf("${", pos_end + 1);
            }
            builder.append(str, pos_end + 1, str.length());
            result = builder.toString();
        }
        return result;
    }


    /**
     * Initialize daemon.
     *
     */
    public void init()
        throws Exception
    {

        // Set Catalina path
        // 设置 catalinaHome
        setCatalinaHome();
        // 设置 Catalina 工作目录
        setCatalinaBase();

        // 初始化类加载器,重要的一个步骤
        // tomcat 独特的类加载器, 违背双亲委派模型
        initClassLoaders();

        // 将设置线程上下文类加载器.
        Thread.currentThread().setContextClassLoader(catalinaLoader);

        SecurityClassLoad.securityClassLoad(catalinaLoader); // 线程安全的加载 class  负责加载Tomcat容器所需的class 检查是否安全, 不安全直接结束

        // Load our startup class and call its process() method
        if (log.isDebugEnabled())
            log.debug("Loading startup class");
        Class<?> startupClass =
            catalinaLoader.loadClass
            ("org.apache.catalina.startup.Catalina");// 加载主类
        Object startupInstance = startupClass.newInstance();// 反射获取实例

        // Set the shared extensions class loader
        if (log.isDebugEnabled())
            log.debug("Setting startup class properties");
        String methodName = "setParentClassLoader";
        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Class.forName("java.lang.ClassLoader");// 创建一个抽象类类加载器类对象
        Object paramValues[] = new Object[1]; // 创建一个对象数组
        paramValues[0] = sharedLoader; // 对象数组中放入分享类加载器
        Method method =
            startupInstance.getClass().getMethod(methodName, paramTypes);// 从Catalina 类获取setParentClassLoader方法对象,
        method.invoke(startupInstance, paramValues);//  调用该方法,传入 sharedLoader , Catalina.parentClassLoader = sharedLoader

        catalinaDaemon = startupInstance;// catalinaDaemon = new Catalina();

    }


    /**
     * Load daemon.
     */
    private void load(String[] arguments)
        throws Exception {

        // Call the load() method
        String methodName = "load";
        Object param[];
        Class<?> paramTypes[];
        if (arguments==null || arguments.length==0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method =
            catalinaDaemon.getClass().getMethod(methodName, paramTypes);
        if (log.isDebugEnabled())
            log.debug("Calling startup class " + method);
        method.invoke(catalinaDaemon, param);

    }


    /**
     * getServer() for configtest
     */
    private Object getServer() throws Exception {

        String methodName = "getServer";
        Method method =
            catalinaDaemon.getClass().getMethod(methodName);
        return method.invoke(catalinaDaemon);

    }


    // ----------------------------------------------------------- Main Program


    /**
     * Load the Catalina daemon.
     */
    public void init(String[] arguments)
        throws Exception {

        init();
        load(arguments);

    }


    /**
     * Start the Catalina daemon.
     */
    public void start()
        throws Exception {
        if( catalinaDaemon==null ) init();

        Method method = catalinaDaemon.getClass().getMethod("start", (Class [] )null);
        method.invoke(catalinaDaemon, (Object [])null);

    }


    /**
     * Stop the Catalina Daemon.
     */
    public void stop()
        throws Exception {

        Method method = catalinaDaemon.getClass().getMethod("stop", (Class [] ) null);
        method.invoke(catalinaDaemon, (Object [] ) null);

    }


    /**
     * Stop the standalone server.
     */
    public void stopServer()
        throws Exception {

        Method method =
            catalinaDaemon.getClass().getMethod("stopServer", (Class []) null);
        method.invoke(catalinaDaemon, (Object []) null);

    }


   /**
     * Stop the standalone server.
     */
    public void stopServer(String[] arguments)
        throws Exception {

        Object param[];
        Class<?> paramTypes[];
        if (arguments==null || arguments.length==0) {
            paramTypes = null;
            param = null;
        } else {
            paramTypes = new Class[1];
            paramTypes[0] = arguments.getClass();
            param = new Object[1];
            param[0] = arguments;
        }
        Method method =
            catalinaDaemon.getClass().getMethod("stopServer", paramTypes);
        method.invoke(catalinaDaemon, param);

    }


    /**
     * Set flag.
     */
    public void setAwait(boolean await)
        throws Exception {

        Class<?> paramTypes[] = new Class[1];
        paramTypes[0] = Boolean.TYPE;
        Object paramValues[] = new Object[1];
        paramValues[0] = Boolean.valueOf(await);
        Method method =
            catalinaDaemon.getClass().getMethod("setAwait", paramTypes);
        method.invoke(catalinaDaemon, paramValues);

    }

    public boolean getAwait()
        throws Exception
    {
        Class<?> paramTypes[] = new Class[0];
        Object paramValues[] = new Object[0];
        Method method =
            catalinaDaemon.getClass().getMethod("getAwait", paramTypes);
        Boolean b=(Boolean)method.invoke(catalinaDaemon, paramValues);
        return b.booleanValue();
    }


    /**
     * Destroy the Catalina Daemon.
     */
    public void destroy() {

        // FIXME

    }


    /**
     * Main method and entry point when starting Tomcat via the provided
     * scripts.
     * 通过提供的脚本启动Tomcat时，主要方法和入口点。
     *
     *
     * @param args Command line arguments to be processed
     */
    public static void main(String args[]) {

        System.err.println("Have fun and Enjoy! cxs");

        // daemon 就是 bootstrap
        if (daemon == null) {
            // Don't set daemon until init() has completed 不要在init()完成之前设置守护者
            // 如果守护线程是 null 创建一个 bootstrap
            Bootstrap bootstrap = new Bootstrap();
            try {
                // 初始化
                bootstrap.init();
            } catch (Throwable t) {
                // 处理异常(抛出)
                handleThrowable(t);
                // 打印异常
                t.printStackTrace();
                // 结束
                return;
            }
            // 初始化结束后, 设置bootstrap 为守护者
            daemon = bootstrap;
        } else {
            // When running as a service the call to stop will be on a new
            // thread so make sure the correct class loader is used to prevent
            // a range of class not found exceptions.
            /*
            当作为一个服务运行时，停止的呼叫将会在一个新的
            线程，确保正确的类装入器用于防止
            一系列的类没有发现异常。
             */

            // 如果已经初始化过了, 则将该线程的类加载器设置为 bootstrap 的类加载器
            Thread.currentThread().setContextClassLoader(daemon.catalinaLoader);
        }

        try {
            // 命令
            String command = "start";
            // 如果命令行中输入了参数
            if (args.length > 0) {
                // 命令 = 最后一个命令
                command = args[args.length - 1];
            }
            // 如果命令是启动
            if (command.equals("startd")) {
                args[args.length - 1] = "start";
                daemon.load(args);
                daemon.start();
            }
            // 如果命令是停止了
            else if (command.equals("stopd")) {
                args[args.length - 1] = "stop";
                daemon.stop();
            }
            // 如果命令是启动
            else if (command.equals("start")) {
                daemon.setAwait(true);// bootstrap 和 Catalina 一脉相连, 这里设置, 方法内部设置 Catalina 实例setAwait方法
                daemon.load(args);// args 为 空,方法内部调用 Catalina 的 load 方法.
                daemon.start();// 相同, 反射调用 Catalina 的 start 方法 ,至此,启动结束
            } else if (command.equals("stop")) {
                daemon.stopServer(args);
            } else if (command.equals("configtest")) {
                daemon.load(args);
                if (null==daemon.getServer()) {
                    System.exit(1);
                }
                System.exit(0);
            } else {
                log.warn("Bootstrap: command \"" + command + "\" does not exist.");
            }
        } catch (Throwable t) {
            // Unwrap the Exception for clearer error reporting
            if (t instanceof InvocationTargetException &&
                    t.getCause() != null) {
                t = t.getCause();
            }
            handleThrowable(t);
            t.printStackTrace();
            // 非正常退出
            System.exit(1);
        }



    }

    public void setCatalinaHome(String s) {
        System.setProperty(Globals.CATALINA_HOME_PROP, s);
    }

    public void setCatalinaBase(String s) {
        System.setProperty(Globals.CATALINA_BASE_PROP, s);
    }


    /**
     * Set the <code>catalina.base</code> System property to the current
     * working directory if it has not been set.
     */
    private void setCatalinaBase() {

        // 获取到 Catalina-home
        if (System.getProperty(Globals.CATALINA_BASE_PROP) != null) {
            return;
        }
        if (System.getProperty(Globals.CATALINA_HOME_PROP) != null) {
            System.setProperty(Globals.CATALINA_BASE_PROP,
                System.getProperty(Globals.CATALINA_HOME_PROP));
        } else {
            System.setProperty(Globals.CATALINA_BASE_PROP,
                System.getProperty("user.dir"));
        }

    }


    /**
     * Set the <code>catalina.home</code> System property to the current
     * working directory if it has not been set.
     *
     * catalina home 默认是 tomcat 根目录
     */
    private void setCatalinaHome() {

        // 获取到 Catalina-home
        if (System.getProperty(Globals.CATALINA_HOME_PROP) != null)
            return;
        // 如果获取不到 Catalina Home
        // 从项目根目录获取 jar 包
        File bootstrapJar =
            new File(System.getProperty("user.dir"), "bootstrap.jar");
        // 如果 jar 存在
        if (bootstrapJar.exists()) {
            try {
              // 设置 Catalina Home 为刚刚查询到的 jar 包 路径
                System.setProperty
                    (Globals.CATALINA_HOME_PROP,
                     (new File(System.getProperty("user.dir"), ".."))
                     .getCanonicalPath());
            } catch (Exception e) {
                // Ignore
              // 如果错误则设置项目根目录路
                System.setProperty(Globals.CATALINA_HOME_PROP,
                                   System.getProperty("user.dir"));
            }
        } else {
          // 如果 jar 不存在, 设置项目根目录
            System.setProperty(Globals.CATALINA_HOME_PROP,
                               System.getProperty("user.dir"));
        }

    }


    /**
     * Get the value of the catalina.home environment variable.
     */
    public static String getCatalinaHome() {
        return System.getProperty(Globals.CATALINA_HOME_PROP,
                                  System.getProperty("user.dir"));
    }


    /**
     * Get the value of the catalina.base environment variable.
     */
    public static String getCatalinaBase() {
        return System.getProperty(Globals.CATALINA_BASE_PROP, getCatalinaHome());
    }


    // Copied from ExceptionUtils since that class is not visible during start
    private static void handleThrowable(Throwable t) {
        if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }
}
