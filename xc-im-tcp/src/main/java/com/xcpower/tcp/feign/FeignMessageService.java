package com.xcpower.tcp.feign;


import com.xcpower.im.ResponseVO;
import com.xcpower.im.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;
import org.springframework.validation.annotation.Validated;

public interface FeignMessageService {

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /message/checkSend")
    ResponseVO checkSendMessage(CheckSendMessageReq o);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /group/message/checkSend")
    ResponseVO checkSend(CheckSendMessageReq req);

}
