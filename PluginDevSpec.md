#ShadowsocksR Droid 协议和混淆插件开发规范v1.0

### ShadowSocksR（下称SSR）插件分为```协议```和```混淆```两种。

####开发步骤

#####准备：

1. 挑选您熟悉的语言：C，C++，Java，Kotlin。

2. 如果使用C/C++则需要利用JNI间接调用。

3. 如果使用Java或Kotlin则可以直接上手了。

#####协议插件：

1. 协议插件应写在com.proxy.shadowsocksr.impl.plugin.proto包下，并继承AbsProtocol类。根据协议类型，类名以Verify或Auth开头，以Protocol结尾。如：AuthSimpleProtocol。

2. AbsProtocol类已保存构造器参数，子类可以直接访问，如无特殊需要则无需再次保存。

3. shareParam用于存储协议用于每个连接的公用数据。

4. 在资源文件arrays.xml中，tcp\_protocol\_entry为协议插件对用户显示的名字，tcp\_protocol\_value为协议插件被App识别的名字，二者顺序要对应。

5. 修改ProtocolChooser类，使其可以识别您写的协议或混淆插件。

#####混淆插件：

1. 混淆插件应写在com.proxy.shadowsocksr.impl.plugin.obfs包下，并继承AbsObfs类。以Obfs结尾。如：HttpSimpleObfs。

2. AbsObfs类已保存构造器参数，子类可以直接访问，如无特殊需要则无需再次保存。

3. 在资源文件arrays.xml中，obfs\_method\_entry为混淆插件对用户显示的名字，obfs\_method\_value为混淆插件被App识别的名字，二者顺序要对应。

4. 修改ObfsChooser类，使其可以识别您写的协议或混淆插件。

5. 如果您的混淆插件需要用户输入字符串参数，请修改PrefFragment类的configSpecialPref()、setPrefEnabled(boolean isEnable)以及loadCurrentPref()方法，以便App可以正确启用EditTextPreference供用户输入。

####提示

1. 由于添加了协议，可能会产生由协议本身导致的断包，粘包的问题，这些问题需由插件自己处理。

2. 尽可能减少异常抛出，不必要的内存拷贝以保证效率。

3. 在数据解析出错时，返回byte\[0\]而不是null。

####如果您既想使用我们开发的SSR Droid，又想用自己写的插件（或是协议，混淆的思路）并且愿意公开它，那么欢迎Pull Request或通过任何你能联系到我们的方式来提交您的智慧结晶，我们会视情况将其加入并发布出来。
