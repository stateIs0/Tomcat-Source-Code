### Server 

```$xslt
Server是Tomcat中最顶层的组件，它可以包含多个Service组件。在Tomcat源代码中Server组件对应源码中的
`org.apache.catalina.core.StandardServer`类

```


### Service

```
Service组件相当于Connetor和Engine组件的包装器，它将一个或者多个Connector组件和一个Engine建立关联。
缺省的的配置文件中，定义一个叫Catalina的服务，并将Http,AJP这两个Connector关联到了一个名为Catalina的
Engine.Service组件对应Tomcat源代码中的org.apache.catalina.core.StandardService
```

### Connector
```$xslt
既然Tomcat需要提供http服务，而我们知道http应用层协议最终都是需要通过TCP层的协议进行包传递的，
而Connector正是Tomcat中监听TCP网络连接的组件，一个Connector会监听一个独立的端口来处理来自客户端的连接。
缺省的情况下Tomcat提供了如下两个Connector。我们分别描述一下：

HTTP/1.1
<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" /> 
上面定义了一个Connector，它缺省监听端口8080,这个端口我们可以根据具体情况进行改动。connectionTimeout定义了连接超时时间，
单位是毫秒，redirectPort定义了ssl的重定向接口，根据缺省的配置，Connector会将ssl请求重定向到8443端口。

AJP/1.3
AJP表示Apache Jserv Protocol,此连接器将处理Tomcat和Aapache http服务器之间的交互，
这个连机器是用来处理我们将Tomcat和Apache http服务器结合使用的情况。假如在同样的一台物理Server上面部署了
一台Apache http服务器和多台Tomcat服务器，通过Apache服务器来处理静态资源以及负载均衡的时候，
针对不同的Tomcat实例需要AJP监听不同的端口。
Connector对应源代码中的org.apache.catalina.connector.Connector

```


### Engine

```$xslt
Tomcat中有一个容器的概念，而Engine,Host,Context都属于Contanier，我们先来说说最顶层的容器Engine.
一个Engine可以包含一个或者多个Host,也就是说我们一个Tomcat的实例可以配置多个虚拟主机。
缺省的情况下<Engine name="Catalina" defaultHost="localhost">定义了一个名称为Cataline
的Engine.Engine对应源代码中的org.apache.catalina.core.StandardEngine

```


### Host
```$xslt
Host定义了一个虚拟主机，一个虚拟主机可以有多个Context，缺省的配置如下：
<Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true">….</Host> 
其中appBase为webapps，也就是<CATALINA_HOME>\webapps目录，unpackingWARS属性指定在appBase指定的
目录中的war包都自动的解压，缺省配置为true,autoDeploy属性指定是否对加入到appBase目录的war包进行自动的部署，缺省为true.

Host对应源代码中的org.apache.catalina.core.StandardHost


```

### Context

````$xslt
在Tomcat中，每一个运行的webapp其实最终都是以Context的形成存在，每个Context都有一个根路径和请求URL路径，
Context对应源代码中的org.apache.catalina.core.StandardContext,
在Tomcat中我们通常采用如下的两种方式创建一个Context.下面分别描述一下：

1. 在<CATALINA-HOME>\webapps目录中创建一个目录，这个时候将自动创建一个context，
默认context的访问url为http://host:port/dirname,你也可以通过在ContextRoot\META-INF
中创建一个context.xml的文件，其中包含如下的内容来指定应用的访问路径。
 <Context path="/yourUrlPath" />
 
2. conf\server.xml文件中增加context元素。 第二种创建context的方法，
我们可以选择在server.xml文件的<Host>元素，比如我们在server.xml文件中增加如下内容：
server.xml

    <Context path="/mypath" docBase="/Users/tiger/develop/xxx" reloadable="true">
    </Context>
  </Host>
</Engine>
  </Service>
</Server>

这样的话，我们就可以通过http://host:port/mypath访问上面配置的context了。

````


### Valve （其实还有一个pipeline 管道)

````$xslt
Valve中文意思是阀门，Valve是Tomcat中责任链模式的实现，通过链接多个Valve对请求进行处理。
其中Valve可以定义在任何的Container中，上面说的Engine,Host,Context都属于容器
。tomcat 默认定义了一个名为org.apache.catalina.valves.AccessLogValve的Valve,
这个Valve负责拦截每个请求，然后记录一条访问日志。


````


通过上面的分析，我们发现Server,Service,Engine,Host,Context都实现了org.apache.catalina.Lifecycle接口，
通过这个接口管理了这些核心组件的生命周期

