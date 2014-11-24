package org.keycloak.proxy;

import io.undertow.Undertow;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String jsonConfig = "proxy.json";
        if (args.length > 0) jsonConfig = args[0];
        File file = new File(jsonConfig);
        if (!file.exists()) {
            System.err.println("No proxy config argument and could not find default file proxy.json");
            System.exit(1);
            return;
        }
        FileInputStream fis = new FileInputStream(file);
        Undertow proxyServer = ProxyServerBuilder.build(fis);
        proxyServer.start();

    }
}
