package com.xcpower.codec;

import com.alibaba.fastjson.JSONObject;
import com.xcpower.codec.proto.Message;
import com.xcpower.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.json.JsonObjectDecoder;

import java.util.List;

/**
 * 消息解码类
 */
public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          ByteBuf in,
                          List<Object> list) throws Exception {

        //28 + imei + body
        //请求头（
        // 指令
        // 版本
        // clientType
        // 消息解析类型
        // imei长度
        // appId
        // bodylen）+ imei号 + 请求体
        //len+body
        if (in.readableBytes() < 28) {
            return;
        }
        // 获取指令
        int command = in.readInt();
        // 获取版本号
        int version = in.readInt();
        // 获取ClientType
        int clientType = in.readInt();
        // 获取消息类型
        int messageType = in.readInt();
        // 获取appId
        int appId = in.readInt();
        // 获取imeiLength
        int imeiLength = in.readInt();
        // 获取bodyLen
        int bodyLen = in.readInt();

        if (in.readableBytes() < imeiLength + bodyLen) {
            in.resetReaderIndex();
            return;
        }
        // 获取imei数据
        byte[] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        // 获取body数据
        byte[] bodyData = new byte[bodyLen];
        in.readBytes(bodyData);
        String body = new String(bodyData);

        MessageHeader messageHeader = MessageHeader.builder()
                .appId(appId)
                .clientType(clientType)
                .command(command)
                .messageType(messageType)
                .imei(imei)
                .length(bodyLen)
                .version(version)
                .build();

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if(messageType == 0){
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        in.markReaderIndex();
        list.add(message);

    }
}
