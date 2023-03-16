package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.extension.ExtensionLoader;

/**
 * @author liuqingwen
 * @version 1.0
 * @Desc
 * @date 2023/3/2 14:59
 */
public class ExtensionTest {

    public static void main(String[] args) {
        ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
    }

}
