package org.app.controller;

import org.app.common.ResponseVO;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@RestController
public class CallbackController {

    private static Logger logger
            = LoggerFactory.getLogger(CallbackController.class);

    @RequestMapping("/callback")
    public ResponseVO callback(@RequestBody Object req, String command, Integer appId) {
        logger.info("{}收到{}回调数据：{}",
                appId,command, JSONObject.toJSONString(req));
        return ResponseVO.successResponse();
    }
}
