package com.xcpower.tcp;

import com.xcpower.codec.config.BootstrapConfig;
import com.xcpower.codec.proto.Message;
import com.xcpower.tcp.reciver.MessageReciver;
import com.xcpower.tcp.redis.MqManager;
import com.xcpower.tcp.redis.RedisManager;
import com.xcpower.tcp.register.RegisterZK;
import com.xcpower.tcp.register.ZKit;
import com.xcpower.tcp.server.LimServer;
import com.xcpower.tcp.server.LimWebSocketServer;
import com.xcpower.tcp.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Starter {


    //client IOS 安卓 pc(windows mac) web //支持json 也支持 protobuf
    //appId
    //28 + imei + body
    //请求头（指令 版本 clientType 消息解析类型 imei长度 appId bodylen）+ imei号 + 请求体
    //len+body

    public static void main(String[] args) {
        if (args.length >= 1) {
            start(args[0]);
        }
    }

    private static void start(String configFilePath) {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFilePath);
            BootstrapConfig bootstrapConfig = yaml.loadAs(inputStream, BootstrapConfig.class);

            new LimServer(bootstrapConfig.getLim()).start();
            new LimWebSocketServer(bootstrapConfig.getLim()).start();

            RedisManager.init(bootstrapConfig);

            MqFactory.init(bootstrapConfig.getLim().getRabbitmq());

            // 开启消息接受，处理用户数据同步
            MessageReciver.init(bootstrapConfig.getLim().getBrokerId().toString());

            // 监听用户上线，处理多端登录
            MqManager.listenUserLoginMessage(bootstrapConfig.getLim().getLoginModel());

            registerZK(bootstrapConfig);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }

    public static void registerZK(BootstrapConfig bootstrapConfig) throws UnknownHostException {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        ZkClient zkClient = new ZkClient(bootstrapConfig.getLim().getZkConfig().getZkAddr(),
                bootstrapConfig.getLim().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegisterZK registerZK = new RegisterZK(zKit, hostAddress, bootstrapConfig.getLim());
        new Thread(registerZK).start();

    }

}
