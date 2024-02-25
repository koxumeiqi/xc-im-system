package com.xcpower.tcp.register;

import com.xcpower.codec.config.BootstrapConfig;
import com.xcpower.im.constant.Constants;
import com.xcpower.tcp.server.LimServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterZK implements Runnable{

    public static final Logger LOGGER = LoggerFactory.getLogger(RegisterZK.class);

    private ZKit zKit;

    private String ip;

    private BootstrapConfig.TcpConfig tcpConfig;

    public RegisterZK(ZKit zKit, String ip, BootstrapConfig.TcpConfig tcpConfig) {
        this.zKit = zKit;
        this.ip = ip;
        this.tcpConfig = tcpConfig;
    }

    @Override
    public void run() {

        zKit.createRootNode();
        String tcpPath = Constants.ImCoreZkRoot + Constants.ImCoreZkRootTcp + "/" + ip + ":" + tcpConfig.getTcpPort();
        zKit.createNode(tcpPath);
        LOGGER.info("Registry zookeeper tcpPath success,msg = [{}]",tcpPath);

        String webPath =
                Constants.ImCoreZkRoot + Constants.ImCoreZkRootWeb + "/" + ip + ":" + tcpConfig.getWebSocketPort();
        zKit.createNode(webPath);
        LOGGER.info("Registry zookeeper webPath success, msg=[{}]", webPath);

    }
}
