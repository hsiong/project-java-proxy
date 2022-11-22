package tech.ynfy.module.proxy;

import tech.ynfy.module.proxy.thread.SocketHandle;

import java.io.IOException;

/**
 * 〈〉
 *
 * @author Hsiong
 * @version 1.0.0
 * @since 2022/11/22
 */
public class HttpProxyRun {

    public static void main(String[] args) throws IOException {
        int port = 8881;
        if (args != null && args.length > 0 && args[0].matches("\\d+")) {
            port = Integer.parseInt(args[0]);
        }
        new SocketHandle(port).start();
    }

}
