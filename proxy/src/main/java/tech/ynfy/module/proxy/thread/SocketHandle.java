package tech.ynfy.module.proxy.thread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 〈〉
 *
 * @author Hsiong
 * @version 1.0.0
 * @since 2022/11/22
 */
public class SocketHandle extends Thread {

    private final ServerSocket server;
    private final int port;

    public SocketHandle(int port) throws IOException {
        this.port = port;
        server = new ServerSocket(port);
        System.out.println("代理端口：" + this.port);
    }

    @Override
    public void run() {
        // 线程运行函数
        while (true) {
            try {
                Socket client = server.accept();
                //使用线程处理收到的请求
                new HttpConnectThread(client).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
