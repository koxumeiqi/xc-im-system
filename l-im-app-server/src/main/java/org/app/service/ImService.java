package org.app.service;

import com.alibaba.fastjson.JSONObject;
import org.app.common.ResponseVO;
import org.app.config.AppConfig;
import org.app.dao.User;
import org.app.model.dto.ImUserDataDto;
import org.app.model.proto.GetUserInfoProto;
import org.app.model.proto.ImportUserProto;
import org.app.model.resp.ImportUserResp;
import org.app.utils.HttpRequestUtils;
import org.app.utils.SigAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Chackylee
 * @description:
 **/
@Service
public class ImService implements CommandLineRunner {

    @Autowired
    HttpRequestUtils httpRequestUtils;

    @Autowired
    AppConfig appConfig;

    private SigAPI sigAPI;

    public volatile static Map<String, Object> parameter;

    public volatile static Object lock = new Object();

    private String getUrl(String uri) {
        return appConfig.getImUrl() + "/" + appConfig.getImVersion() + uri;
    }

    private Map<String, Object> getParamter() {
        if (parameter == null) {
            synchronized (lock) {
                if (parameter == null) {
                    parameter = new ConcurrentHashMap<>();
                    parameter.put("appId", appConfig.getAppId());
                    parameter.put("userSign", sigAPI.genUserSig(appConfig.getAdminId(),
                            500000));
                    parameter.put("identifier", appConfig.getAdminId());
                }
            }
        }
        return parameter;
    }

    public ResponseVO<ImportUserResp> importUser(List<User> users) {

        ImportUserProto proto = new ImportUserProto();
        List<ImportUserProto.UserData> userData = new ArrayList<>();
        users.forEach(e -> {
            ImportUserProto.UserData u = new ImportUserProto.UserData();
            u.setUserId(e.getUserId().toString());
            u.setUserType(1);
            userData.add(u);
        });

        String uri = "/user/importUser";
        try {
            proto.setUserData(userData);
            ResponseVO responseVO = httpRequestUtils.doPost(
                    getUrl(uri),
                    ResponseVO.class,
                    getParamter(),
                    null,
                    JSONObject.toJSONString(proto), "");
            return responseVO;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseVO.errorResponse();
    }


    public ResponseVO<ImUserDataDto> getUserInfo(List<String> users) {

        GetUserInfoProto proto = new GetUserInfoProto();
        proto.setUserIds(users);

        String uri = "/user/data/getUserInfo";
        try {
//            proto.setUserData(userData);
            ResponseVO responseVO = httpRequestUtils.doPost(getUrl(uri), ResponseVO.class, getParamter(), null, JSONObject.toJSONString(proto), "");
            return responseVO;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.errorResponse();
    }


    @Override
    public void run(String... args) throws Exception {
        sigAPI = new SigAPI(appConfig.getAppId(), appConfig.getPrivateKey());
    }
}
