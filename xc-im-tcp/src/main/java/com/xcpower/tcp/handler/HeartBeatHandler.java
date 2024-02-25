package com.xcpower.tcp.handler;

import com.xcpower.im.constant.Constants;
import com.xcpower.tcp.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private Long heartBeatTime;

    public HeartBeatHandler(Long heartBeatTime) {
        this.heartBeatTime = heartBeatTime;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断 evt 是否是 IdleStateEvent（用于触发用户事件，包含   读空闲/写空闲/读写空闲）
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.info("进入读空闲...");
            } else if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                log.info("进入写空闲");
            } else if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                Long lastReadTime = (Long) ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).get();
                long now = System.currentTimeMillis();

                if (lastReadTime != null && now - lastReadTime > heartBeatTime) {
                    // TODO 退后台逻辑
                    SessionSocketHolder.offlineUserSession(((NioSocketChannel) ctx.channel()));
                }
            }
        }
    }
}
