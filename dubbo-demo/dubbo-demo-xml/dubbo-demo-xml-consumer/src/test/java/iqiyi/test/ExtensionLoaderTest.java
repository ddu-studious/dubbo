package iqiyi.test;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Protocol;
import org.junit.jupiter.api.Test;

/**
 * @author liuqingwen@qiyi.com
 * @date 2019-12-07 15:15
 */
public class ExtensionLoaderTest {

    @Test
    public void test01() {

        final Protocol adaptiveExtension = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        System.out.println(adaptiveExtension);

    }

}
