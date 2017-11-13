# Tomcat-Source-Code
> 为了学习tomcat源码，并再学习源码时对源码做注释，用于加深理解


### Quick Start!! 快速开始

star 之后点击clone， 使用IDEA 或 eclipse （本人是IDEA）配置 VM options，然后Run Bootstrap


VM options参数：
-Dcatalina.home=catalina-home -Dcatalina.base=catalina-home
-Djava.endorsed.dirs=catalina-home/endorsed -Djava.io.tmpdir=catalina-home/temp
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager
-Djava.util.logging.config.file=catalina-home/conf/logging.properties
