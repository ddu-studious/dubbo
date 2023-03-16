package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.bytecode.Wrapper;

/**
 * @author liuqingwen
 * @version 1.0
 * @Desc
 * @date 2023/3/10 14:55
 */
public class WrapperTest {

    public static void main(String[] args) {

        Wrapper wrapper = Wrapper.getWrapper(K.class);

    }

    private interface K {
        void a();
    }

}
