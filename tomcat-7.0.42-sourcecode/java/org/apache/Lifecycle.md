###  Tomcat生命周期管理

```text
从Tomcat整体架构 中，我们都知道Tomcat中最顶层的容器叫Server，代表着整个服务器，
而Tomcat里的Server有org.apache.catalina.startup.Catalina 来管理，
Catalina是整个Tomcat的管理类，它里面的三个方法load、start、stop分别用于管理整个服务器的生命周期，
这三个方法会按照容器的结构逐层调用相应方法，如Server的start方法中会调用所有的Service的start的start方法
，Service中的start方法又会调用所有包含的Connectors和Container的start方法，
这样整个服务器就启动了。下面我们将来分析这样做的原因。 
Tomcat整体架构这篇文章中列出的组件的继承结构类图都有一个共同之处：
它们都继承org.apache.catalina.util.LifecycleMBeanBase,
而LifecycleMBeanBase又继承自org.apache.catalina.util.LifecycleBase,
LifecycleBase实现了org.apache.catalina.Lifecycle接口。


Lifecycle主要有四个生命周期：init(初始化)、start(启动)、stop(停止)、destroy(销毁)，
知道Lifecycle的四个生命周期之后，接下来我们来看看它的实现类LifecycleBase

```


### 小结Tomcat生命周期的管理
```text
Lifecycle接口定义了Tomcat的四个生命周期：init(初始化)、start(启动)、stop(停止)、destroy(销毁)，
而 LifecycleBase采用模板方法模式来对所有支持生命周期管理的组件的生命周期各个阶段进行了总体管理，
每个有生命周期的组件都直接或间接继承于LifecycleBase类，并实现其中的生命周期的模板方法以加入相应生命周期的流程中
。而Tomcat里的Server有org.apache.catalina.startup.Catalina 来管理，Catalina是整个Tomcat的管理类，
它里面的三个方法load、start、stop分别用于管理整个服务器的生命周期，这三个方法会按照容器的结构逐层调用相应方法，
因为有生命周期的组件都实现了Lifecycle接口。这就是Tomcat生命周期的管理方式。
```


### 总结
```text
对生命周期进行管理的类是org.apahce.catalina.startup.Catalina,
该类中的三个方法load、start、stop正是管理整个Tomcat服务器生命周期的方法，
这三个方法会按照容器结构层次调用调用相应的方法，达到统一启动/关闭这些组件的效果
因为Tomcat的各个组件实现org.apache.catalina.Lifecycle。
而Lifecycle接口定义了Tomcat所有的生命周期方法：init(初始化)、start(启动)、stop(停止)、destroy(销毁)，
其实现类LifecycleBase采用模板方法模式来对所有支持生命周期管理的组件的生命周期各个阶段进行了总体管理，
每个有生命周期的组件都直接或间接继承于LifecycleBase类，并实现其中的生命周期的模板方法以加入相应生命周期的流程中。
这就是Tomcat生命周期的管理方式。 Tomcat的启动时从顶层结构开始，在父层启动过程中调用子层的启动，
从而达到统一启动组件的效果，特别需要注意的是Host层及以下的层都是使用异步线程池来启动子容器，
这样做的目的是减少服务器的启动时间，同时保证所有子容器都启动完毕时才进行下一步操作；
web应用是在Host启动时通过事件Lifecycle.START_EVENT的监听器HostConfig来启动的。 
Tomcat提供两种关闭服务器的方式：一种是利用虚拟机的关闭钩子机制，在虚拟机关闭时关闭各个组件，
进而关闭服务器；另一种通过监听网络端口8005传递的SHUTDOWN`指令，关闭各个组件。
```
