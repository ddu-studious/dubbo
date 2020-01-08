package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author liuqingwen@qiyi.com
 * @date 2020-01-08 10:51
 */
public class ConsumerLoadBalance extends RandomLoadBalance {

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        LoggerFactory.getLogger("ConsumerLoadBalance").error("consumer load balance");
        return super.doSelect(invokers, url, invocation);
    }
}
