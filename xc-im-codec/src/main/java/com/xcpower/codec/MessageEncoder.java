package com.xcpower.codec;

import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.proto.MessagePack;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import sun.util.resources.cldr.ga.CurrencyNames_ga;

/**
 * 消息编码类，私有协议规则，前4位表示长度，接着command4位，后面是数据
 */
public class MessageEncoder extends MessageToByteEncoder {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          Object msg,
                          ByteBuf byteBuf) throws Exception {
        if(msg instanceof MessagePack){
            MessagePack<?> msgBody = (MessagePack<?>) msg;
            String s = JSONObject.toJSONString(msgBody.getData());
            byte[] bytes = s.getBytes();
            byteBuf.writeInt(msgBody.getCommand());
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
    }
}
