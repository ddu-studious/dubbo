package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

/**
 * @author liuqingwen
 * @version 1.0
 * @Desc
 * @date 2023/4/4 19:10
 */
public class StandardAnnotationMetadataTest {

    @Configuration
    @EnableDubbo(scanBasePackages = "org.apache.dubbo.demo.consumer.comp")
    @PropertySource("classpath:/spring/dubbo-consumer.properties")
    @ComponentScan(value = {"org.apache.dubbo.demo.consumer.comp"})
    static class ConsumerConfiguration {

    }

    public static void main(String[] args) throws URISyntaxException, ClassNotFoundException {

        StandardAnnotationMetadata standardAnnotationMetadata = new StandardAnnotationMetadata(ConsumerConfiguration.class, true);
        AnnotationAttributes mergedAnnotationAttributes = AnnotatedElementUtils.findMergedAnnotationAttributes(standardAnnotationMetadata.getIntrospectedClass(), DubboComponentScan.class.getName(), true, true);
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(mergedAnnotationAttributes);
        System.out.println(mergedAnnotationAttributes);
        String[] basePackages = annotationAttributes.getStringArray("basePackages");
        System.out.println(Arrays.toString(basePackages));

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String basePackage : basePackages) {
            URL resource = classLoader.getResource(basePackage.replace(".", "/"));
            URI uri = resource.toURI();
            File file = Paths.get(uri).toFile();
            if (file.exists()) {
                for (String fn : file.list()) {
                    System.out.println(fn);

                    Class<?> aClass = Class.forName(basePackage + "." + fn.replace(".class", ""), true, classLoader);

                    StandardAnnotationMetadata standardAnnotationMetadata1 = new StandardAnnotationMetadata(aClass);
                    Map<String, Object> annotationAttributes1 = standardAnnotationMetadata1.getAnnotationAttributes(Component.class.getName(), true);
                    System.out.println(annotationAttributes1);
                }
            }
        }


    }
}
