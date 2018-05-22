package com.alibaba.dubbo.performance.demo.agent.dubbo.agent.client;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class AgentClientConnecManager {
    Logger logger = LoggerFactory.getLogger(AgentClientConnecManager.class);
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    private ConcurrentHashMap<Endpoint, Channel> channelPool = new ConcurrentHashMap<>();

    private Bootstrap bootstrap;

    private final Object lock = new Object();

    public AgentClientConnecManager() {
    }

    public Channel getChannel(Endpoint agentEndpoint) throws Exception {
        Channel channel = channelPool.get(agentEndpoint);
        if (null != channel) {
            return channel;
        }

        if (null == bootstrap) {
            synchronized (lock) {
                if (null == bootstrap) {
                    initBootstrap();
                }
            }
        }

        if (null == channel) {
            synchronized (lock) {
                if (null == channel) {
                    try {
                        channel = bootstrap.connect(agentEndpoint.getHost(), agentEndpoint.getPort()).sync().channel();
                        logger.info("与{}:{}新建立了连接", agentEndpoint.getHost(), agentEndpoint.getPort());
                        channelPool.put(agentEndpoint, channel);
                    } catch (Exception e) {
                        logger.error("连接失败", e);
                    }

                }
            }
        }

        return channel;
    }

    public void initBootstrap() {

        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
                .channel(NioSocketChannel.class)
                .handler(new AgentClientInitializer());
    }
}
