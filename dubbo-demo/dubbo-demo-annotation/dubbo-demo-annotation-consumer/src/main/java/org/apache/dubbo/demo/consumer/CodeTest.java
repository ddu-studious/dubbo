package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.bytecode.ClassGenerator;

/**
 * @author liuqingwen
 * @version 1.0
 * @Desc
 * @date 2023/3/2 11:47
 */
public class CodeTest {

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {

        ClassGenerator cc = ClassGenerator.newInstance(Thread.currentThread().getContextClassLoader());
        cc.setClassName("Aa");
        cc.setSuperClass(U.class);

        cc.addDefaultConstructor();
        cc.addField("public int i = 1;");
        cc.addMethod("public int get() { return i;}");

        Class<?> aClass = cc.toClass();
        System.out.println(aClass);
        U u = (U) aClass.newInstance();
        System.out.println(u.get());
    }

}
