package com.xcpower.tcp.reciver.process;

import com.xcpower.codec.proto.MessagePack;
import com.xcpower.tcp.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class BaseProcess {

    public abstract void processBefore();

    public void process(MessagePack messagePack){
        processBefore();

        NioSocketChannel channel = SessionSocketHolder.get(messagePack.getAppId(),
                messagePack.getToId(),
                messagePack.getImei(),
                messagePack.getClientType());
        if(channel != null){
            channel.writeAndFlush(messagePack);
        }
        processAfter();
    }

    public abstract void processAfter();

}
