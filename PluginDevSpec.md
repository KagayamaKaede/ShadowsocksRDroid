#ShadowsocksR Droid 协议和混淆插件开发规范v1.0

### ShadowSocksR（下称SSR）插件分为```协议```和```混淆```两种。

####开发步骤：

1. 协议插件应写在com.proxy.shadowsocksr.impl.proto包下，并继承AbsProtocol类。根据协议类型，类名以Verify或Auth开头，以Protocol结尾。如：AuthSimpleProtocol

2. 混淆插件应写在com.proxy.shadowsocksr.impl.obfs包下，并继承AbsObfs类。以Obfs结尾。如：HttpSimpleObfs。

3. AbsObfs、AbsProtocol类均已保存构造器参数，子类可以直接访问，如无特殊需要则无需再次保存。

4. 在资源文件arrays.xml中，tcp\_protocol\_entry为协议插件对用户显示的名字，tcp\_protocol\_value为协议插件被App识别的名字，二者顺序要对应。

5. 在资源文件arrays.xml中，obfs\_method\_entry为混淆插件对用户显示的名字，obfs\_method\_value为混淆插件被App识别的名字，二者顺序要对应。

6. 修改ProtocolChooser或ObfsChooser类，使其可以识别您写的协议或混淆插件。

7. 如果您的混淆插件需要用户输入字符串参数，请修改PrefFragment类的configSpecialPref()、setPrefEnabled(boolean isEnable)以及loadCurrentPref()方法，以便App可以正确启用EditTextPreference供用户输入。

##Tip

1. 由于添加了协议，可能会产生由协议本身导致的断包，粘包的问题，这些需由插件自己处理，因为主程序不向插件暴露Buffer。

####如果您既想使用我们开发的SSR Droid，又想用自己写的插件（或是协议，混淆的思路）并且愿意公开它，那么欢迎Pull Request或通过任何你能联系到我们的方式来提交您的智慧结晶，我们会视情况将其加入并发布出来。
