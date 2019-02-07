# BuryPlugin
无痕埋点统计，字节码插桩; `buryplugin` 该包为插件源码; `burypluginlib` 该包为插件辅助处理功能源码
# 使用配置
#### 1.配置项目的`build.gradle`
  * repositories中添加`maven { url "https://dl.bintray.com/xujia0903/BuryMaven" }`这个是在maven的地址,（__添加jcenter之后无需添加以上信息__)
  * dependencies中添加`classpath 'com.godxj.plugin:buryplugin:1.0.0'`
#### 2.配置项目依赖`build.gradle`
  * dependencies中添加`compile 'com.godxj:burypluginlib:1.0.0'`
  * 添加插件`apply plugin: 'com.godxj.plugin.bury'`
  * 添加插件配置信息：
    * `buryConfig{`
     * `startPlugin = true` //用于启用或停止插件
     * `buryActivities = "com.godxj.babycorrell.base.BaseActivity"` //该字段可选 如果配置该字段 将只埋点配置对应的页面，页面以类名全路径，多个之间用","隔开,建议配置自己写的BaseActivity为埋点,不配置时，默认读取manifest中所有的activity进行埋点
     * `buryFragments = "com.godxj.babycorrell.base.BaseFragment"` //不配置时，将埋点所有继承android.support.v4.app.Fragment 的fragment,该字段可选 如果配置该字段 将只埋点配置对应的页面，页面以类名全路径，多个之间用","隔开,建议配置自己写的BaseFragment为埋点
    * `}`
#### 3.回调处理
**如果不做处理，并且startPlugin为true，plugin任然起作用但是回调的类将是默认的类；点击事件处理默认类为`com.godxj.plugin.SimpleClickProxy`, 页面停留事件处理默认类为`com.godxj.plugin.SimpleTimeProxy`** 
  * 1.自定义处理点击回调，必须定义类并继承`com.godxj.plugin.imp.AbstractOnClickProxy`,抽象方法`clickProxy`作为所有点击事件处理回调的出口，方法参数将回调给用户几个基本信息，具体可查看`BuryClickInfo`该类的成员变量
  * 2.自定义处理页面停留回调，必须定义类并集成`com.godxj.plugin.imp.AbstractTimeProxy`,抽象方法`stayTime`作为页面停留的回调，该方法会被调用多次，每次离开页面都将会被调用；抽象方法`surviveTime`作为页面存活回调，该方法只会被调用一次，当页面关闭时会被调用；
# 实现原理
可直接查看我的个人blog，由于时间问题，可能不会有分析的相关内容，毕竟本人很懒 `http://www.godxj.com/blog` 进去你会发现，连blog网站都是新鲜的
