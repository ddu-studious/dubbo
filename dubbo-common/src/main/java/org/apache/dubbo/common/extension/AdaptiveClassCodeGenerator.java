/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.extension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

/**
 * Code generator for Adaptive class
 */
public class AdaptiveClassCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveClassCodeGenerator.class);

    private static final String CLASSNAME_INVOCATION = "org.apache.dubbo.rpc.Invocation";

    private static final String CODE_PACKAGE = "package %s;\n";

    private static final String CODE_IMPORTS = "import %s;\n";

    private static final String CODE_CLASS_DECLARATION = "public class %s$Adaptive implements %s {\n";

    private static final String CODE_METHOD_DECLARATION = "public %s %s(%s) %s {\n%s}\n";

    private static final String CODE_METHOD_ARGUMENT = "%s arg%d";

    private static final String CODE_METHOD_THROWS = "throws %s";

    private static final String CODE_UNSUPPORTED = "throw new UnsupportedOperationException(\"The method %s of interface %s is not adaptive method!\");\n";

    private static final String CODE_URL_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"url == null\");\n%s url = arg%d;\n";

    private static final String CODE_EXT_NAME_ASSIGNMENT = "String extName = %s;\n";

    private static final String CODE_EXT_NAME_NULL_CHECK = "if(extName == null) "
                    + "throw new IllegalStateException(\"Failed to get extension (%s) name from url (\" + url.toString() + \") use keys(%s)\");\n";

    private static final String CODE_INVOCATION_ARGUMENT_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"invocation == null\"); "
                    + "String methodName = arg%d.getMethodName();\n";


    private static final String CODE_EXTENSION_ASSIGNMENT = "%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);\n";

    private static final String CODE_EXTENSION_METHOD_INVOKE_ARGUMENT = "arg%d";

    private final Class<?> type;

    private String defaultExtName;

    public AdaptiveClassCodeGenerator(Class<?> type, String defaultExtName) {
        this.type = type;
        this.defaultExtName = defaultExtName;
    }

    /**
     * test if given type has at least one method annotated with <code>SPI</code>
     */
    private boolean hasAdaptiveMethod() {
        return Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Adaptive.class));
    }

    /**
     * generate and return class code
     */
    public String generate() { // 以下以获取ProxyFactory自适应类为例的注释
        // no need to generate adaptive class since there's no adaptive method found.
        if (!hasAdaptiveMethod()) {
            throw new IllegalStateException("No adaptive method exist on extension " + type.getName() + ", refuse to create the adaptive class!");
        }

        StringBuilder code = new StringBuilder();
        code.append(generatePackageInfo()); // 包名
        code.append(generateImports()); // 导入ExtensionLoader类
        code.append(generateClassDeclaration()); // 类定义及继承 ProxyFactory

        Method[] methods = type.getMethods(); // ProxyFactory 的方法
        for (Method method : methods) {
            code.append(generateMethod(method)); // 方法组装
        }
        code.append("}");

        if (logger.isDebugEnabled()) {
            logger.debug(code.toString());
        }
        return code.toString();
    }

    /**
     * generate package info
     */
    private String generatePackageInfo() {
        return String.format(CODE_PACKAGE, type.getPackage().getName());
    }

    /**
     * generate imports
     */
    private String generateImports() {
        return String.format(CODE_IMPORTS, ExtensionLoader.class.getName());
    }

    /**
     * generate class declaration
     */
    private String generateClassDeclaration() {
        return String.format(CODE_CLASS_DECLARATION, type.getSimpleName(), type.getCanonicalName());
    }

    /**
     * generate method not annotated with Adaptive with throwing unsupported exception
     */
    private String generateUnsupported(Method method) {
        return String.format(CODE_UNSUPPORTED, method, type.getName());
    }

    /**
     * 找到入参是URL类型的下标
     * get index of parameter with type URL
     */
    private int getUrlTypeIndex(Method method) {
        int urlTypeIndex = -1;
        Class<?>[] pts = method.getParameterTypes();
        for (int i = 0; i < pts.length; ++i) {
            if (pts[i].equals(URL.class)) {
                urlTypeIndex = i;
                break;
            }
        }
        return urlTypeIndex;
    }

    /**
     * generate method declaration
     */
    private String generateMethod(Method method) {
        String methodReturnType = method.getReturnType().getCanonicalName(); // 返回名称
        String methodName = method.getName(); // 方法名
        String methodContent = generateMethodContent(method); // 方法体
        String methodArgs = generateMethodArguments(method); // 方法入参
        String methodThrows = generateMethodThrows(method); // 发放抛出异常
        return String.format(CODE_METHOD_DECLARATION, methodReturnType, methodName, methodArgs, methodThrows, methodContent); // 方法组装
    }

    /**
     * generate method arguments
     */
    private String generateMethodArguments(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length)
                        .mapToObj(i -> String.format(CODE_METHOD_ARGUMENT, pts[i].getCanonicalName(), i))
                        .collect(Collectors.joining(", "));
    }

    /**
     * generate method throws
     */
    private String generateMethodThrows(Method method) {
        Class<?>[] ets = method.getExceptionTypes();
        if (ets.length > 0) {
            String list = Arrays.stream(ets).map(Class::getCanonicalName).collect(Collectors.joining(", "));
            return String.format(CODE_METHOD_THROWS, list);
        } else {
            return "";
        }
    }

    /**
     * generate method URL argument null check
     */
    private String generateUrlNullCheck(int index) {
        return String.format(CODE_URL_NULL_CHECK, index, URL.class.getName(), index); // 如果URL入参为null，则抛出异常，否则拿到URL变量
    }

    /**
     * generate method content
     */
    private String generateMethodContent(Method method) {
        Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class); // ProxyFactory方法的注解
        StringBuilder code = new StringBuilder(512);
        if (adaptiveAnnotation == null) {
            return generateUnsupported(method); // 当实现的方法没有 @Adaptive 注解的时候，直接抛出异常作为方法的实现
        } else {
            int urlTypeIndex = getUrlTypeIndex(method); // 找到入参是URL类型的下标

            // 找到URL变量逻辑
            // found parameter in URL type
            if (urlTypeIndex != -1) {
                // Null Point check
                code.append(generateUrlNullCheck(urlTypeIndex));
            } else {
                // did not find parameter in URL type
                code.append(generateUrlAssignmentIndirectly(method));
            }

            String[] value = getMethodAdaptiveValue(adaptiveAnnotation);

            // 入参是否有 ：org.apache.dubbo.rpc.Invocation 变量
            boolean hasInvocation = hasInvocationArgument(method);

            // 有org.apache.dubbo.rpc.Invocation入参的方法，如果org.apache.dubbo.rpc.Invocation变量为null，则抛出异常
            // 存在则拿  org.apache.dubbo.rpc.Invocation.getMethodName()
            code.append(generateInvocationArgumentNullCheck(method));

            // extName 获取逻辑定义
            code.append(generateExtNameAssignment(value, hasInvocation));
            // check extName == null?
            code.append(generateExtNameNullCheck(value));

            // 通过 ExtensionLoader 获得 type 类型的类
            // ExtensionLoader.getExtensionLoader(type).getExtension(extName)    extName 是通过URL.getParameter()获得的
            // 获得的变量名 extension
            code.append(generateExtensionAssignment());

            // return statement
            // extension.method(args)
            code.append(generateReturnAndInvocation(method));
        }

        return code.toString();
    }

    /**
     * generate code for variable extName null check
     */
    private String generateExtNameNullCheck(String[] value) {
        return String.format(CODE_EXT_NAME_NULL_CHECK, type.getName(), Arrays.toString(value));
    }

    /**
     * generate extName assigment code
     */
    // 以下以ProxyFactory为例
    private String generateExtNameAssignment(String[] value, boolean hasInvocation) {
        // TODO: refactor it
        String getNameCode = null;
        for (int i = value.length - 1; i >= 0; --i) { // value : {"proxy"}
            if (i == value.length - 1) { // 最后一个
                if (null != defaultExtName) { // @SPI.value()的第一个值  ： javassist
                    if (!"protocol".equals(value[i])) { // @Adaptive.value() != "protocol"
                        if (hasInvocation) { // 方法有 org.apache.dubbo.rpc.Invocation 入参
                            // 获取URL上的方法执行
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            // value[i] = "proxy"
                            // defaultExtName = "javassist"
                        } else {
                            // 获取URL上对应的值
                            getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            // value[i] = "proxy"
                            // defaultExtName = "javassist"
                        }
                    } else { // @Adaptive.value() == "protocol"
                            // 如果 url.getProtocol() == null，在使用 defaultExtName="javassist"，否则url.getProtocol()
                        getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                    }
                } else { // @SPI.value() == null
                    if (!"protocol".equals(value[i])) { // 非protocol
                        if (hasInvocation) {
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                        }
                    } else {
                        getNameCode = "url.getProtocol()";
                    }
                }
            } else {
                if (!"protocol".equals(value[i])) {
                    if (hasInvocation) {
                        getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                    } else {
                        getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                    }
                } else {
                    getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                }
            }
        }

        return String.format(CODE_EXT_NAME_ASSIGNMENT, getNameCode);
    }

    /**
     * @return
     */
    private String generateExtensionAssignment() {
        return String.format(CODE_EXTENSION_ASSIGNMENT, type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
    }

    /**
     * generate method invocation statement and return it if necessary
     */
    private String generateReturnAndInvocation(Method method) {
        String returnStatement = method.getReturnType().equals(void.class) ? "" : "return ";

        String args = IntStream.range(0, method.getParameters().length)
                .mapToObj(i -> String.format(CODE_EXTENSION_METHOD_INVOKE_ARGUMENT, i))
                .collect(Collectors.joining(", "));

        return returnStatement + String.format("extension.%s(%s);\n", method.getName(), args);
    }

    /**
     * test if method has argument of type <code>Invocation</code>
     */
    private boolean hasInvocationArgument(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return Arrays.stream(pts).anyMatch(p -> CLASSNAME_INVOCATION.equals(p.getName()));
    }

    /**
     * generate code to test argument of type <code>Invocation</code> is null
     */
    // 有org.apache.dubbo.rpc.Invocation入参的方法，如果org.apache.dubbo.rpc.Invocation变量为null，则抛出异常
    // 存在则拿  org.apache.dubbo.rpc.Invocation.getMethodName()
    private String generateInvocationArgumentNullCheck(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length).filter(i -> CLASSNAME_INVOCATION.equals(pts[i].getName()))
                        .mapToObj(i -> String.format(CODE_INVOCATION_ARGUMENT_NULL_CHECK, i, i))
                        .findFirst().orElse("");
    }

    /**
     * get value of adaptive annotation or if empty return splitted simple name
     */
    private String[] getMethodAdaptiveValue(Adaptive adaptiveAnnotation) {
        String[] value = adaptiveAnnotation.value();
        // value is not set, use the value generated from class name as the key
        if (value.length == 0) {
            String splitName = StringUtils.camelToSplitName(type.getSimpleName(), "."); // 转成小写且前面加一个.
            value = new String[]{splitName};
        }
        return value;
    }

    /**
     * get parameter with type <code>URL</code> from method parameter:
     * <p>
     * test if parameter has method which returns type <code>URL</code>
     * <p>
     * if not found, throws IllegalStateException
     */
    private String generateUrlAssignmentIndirectly(Method method) {
        Class<?>[] pts = method.getParameterTypes();

        // find URL getter method
        for (int i = 0; i < pts.length; ++i) {
            for (Method m : pts[i].getMethods()) { // 入参类型的方法遍历
                String name = m.getName();
                if ((name.startsWith("get") || name.length() > 3) // get方法且方法名长度大于3 例：getA
                        && Modifier.isPublic(m.getModifiers()) // 公开
                        && !Modifier.isStatic(m.getModifiers()) // 非静态方法
                        && m.getParameterTypes().length == 0 // 参数类型数量=0
                        && m.getReturnType() == URL.class) { // 返回类型是 URL 类型
                    return generateGetUrlNullCheck(i, pts[i], name);
                }
            }
        }

        // getter method not found, throw
        throw new IllegalStateException("Failed to create adaptive class for interface " + type.getName()
                        + ": not found url parameter or url attribute in parameters of method " + method.getName());

    }

    /**
     * 判断URL是否为空
     * 1, test if argi is null
     * 2, test if argi.getXX() returns null
     * 3, assign url with argi.getXX()
     */
    private String generateGetUrlNullCheck(int index, Class<?> type, String method) { // 下标 、 可返回URL的类、可返回URL的方法
        // Null point check
        StringBuilder code = new StringBuilder();
        code.append(String.format("if (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");\n",
                index, type.getName())); // 如果可返回URL的入参==null，则抛出异常
        code.append(String.format("if (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");\n",
                index, method, type.getName(), method)); // 如果可以返回URL的入参执行可返回URL的方法，返回null，则抛出异常

        code.append(String.format("%s url = arg%d.%s();\n", URL.class.getName(), index, method)); // 取到URL变量
        return code.toString();
    }

}
