package tech.ynfy.module.proxy.thread;

import tech.ynfy.module.proxy.util.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 〈新连接处理线程〉
 *
 * @author Hsiong
 * @version 1.0.0
 * @since 2022/11/22
 */
public class HttpConnectThread extends Thread {

    private Socket client;
    private Socket server = null;
    private String host = null;
    private String socketHost = null;
    private Integer socketPort = null;
    private int clientReadLength = 0;
    byte clientInputBuffer[] = new byte[1024 * 1024 * 4];
    private DataInputStream clientInputStream = null; //客户端输入流
    private DataInputStream serverInputStream = null; //服务端输入流
    private DataOutputStream clientOutputStream = null; //客户端输出流
    private DataOutputStream serverOutputStream = null;  //服务端输出流
    private long createTime = System.currentTimeMillis();
    private String clientInputString = null;

    public HttpConnectThread(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {

        try {
            clientInputStream = new DataInputStream(client.getInputStream());
            clientOutputStream = new DataOutputStream(client.getOutputStream());
            if (clientInputStream != null && clientOutputStream != null) {
                clientReadLength = clientInputStream.read(clientInputBuffer, 0, clientInputBuffer.length); // 从客户端读数据
                if (clientReadLength > 0) { // 读到数据
                    clientInputString = new String(clientInputBuffer, 0, clientReadLength);
                    if (clientInputString.contains("\n")) {
                        clientInputString = clientInputString.substring(0, clientInputString.indexOf("\n"));
                    }
                    if (clientInputString.contains("CONNECT ")) {
                        parseServerHost("CONNECT ([^ ]+) HTTP/");
                    } else if (clientInputString.contains("http://") && clientInputString.contains("HTTP/")) {
                        // 从所读数据中取域名和端口号
                        parseServerHost("http://([^/]+)/");
                    }
                    if (host != null & this.socketPort != null) {
                        server = new Socket(socketHost, this.socketPort);
                        System.out.println("clientInputString: " + clientInputString);
                        // 根据读到的域名和端口号建立套接字
                        serverInputStream = new DataInputStream(server.getInputStream());
                        serverOutputStream = new DataOutputStream(server.getOutputStream());
                        if (serverInputStream != null && serverOutputStream != null && server != null) {
                            if (clientInputString.contains("CONNECT ")) {
                                doConnect();
                                return;
                            }
                            doRequest();
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
                            System.err.println(this.getClass().getName() +
                                   " : " +
                                   Thread.currentThread().getStackTrace()[1].getMethodName() +
                                   " : " +
                                   e.getMessage());
        } finally {
            IOUtils.close(serverInputStream, serverOutputStream, server, clientInputStream, clientOutputStream, client);
        }
    }

    /**
     * 解析主机地址
     *
     * @param regExp
     */
    private void parseServerHost(String regExp) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher matcher = pattern.matcher(clientInputString + "/");
        if (matcher.find()) {
            host = matcher.group(1);
            if (host.contains(":")) {
                socketPort = Integer.parseInt(host.substring(host.indexOf(":") + 1));
                socketHost = host.substring(0, host.indexOf(":"));
            }
        }
        if (socketHost != null && socketHost.contains("37")) {
            // 转发
//            socketHost = socketHost.replace("139", "151");
        }
    }

    /**
     * 处理请求
     *
     * @throws IOException
     */
    private void doRequest() throws IOException, InterruptedException {
        serverOutputStream.write(clientInputBuffer, 0, clientReadLength);
        serverOutputStream.flush();
        final CountDownLatch latch;
        if (clientInputString.contains("POST ")) {
            latch = new CountDownLatch(2);
            // 建立线程 , 用于从内网读数据 , 并返回给外网
            new HttpChannel(clientInputStream, serverOutputStream, latch).start();
        } else {
            latch = new CountDownLatch(1);
        }
        // 建立线程 , 用于从外网读数据 , 并返回给内网
        new HttpChannel(serverInputStream, clientOutputStream, latch).start();
        latch.await(5, TimeUnit.SECONDS);
        IOUtils.close(serverInputStream, serverOutputStream, server, clientInputStream, clientOutputStream, client);
        System.out.println("耗时：" + (System.currentTimeMillis() - createTime) + "ms; clientInputString " + clientInputString);
    }

    /**
     * 处理连接请求
     *
     * @return
     */
    private void doConnect() throws IOException, InterruptedException {
        String ack = "HTTP/1.0 200 Connection established\r\n";
        ack = ack + "Proxy-agent: proxy\r\n\r\n";
        clientOutputStream.write(ack.getBytes());
        clientOutputStream.flush();
        final CountDownLatch latch = new CountDownLatch(2);
        // 建立线程 , 用于从外网读数据 , 并返回给内网
        new HttpChannel(serverInputStream, clientOutputStream, latch).start();
        // 建立线程 , 用于从内网读数据 , 并返回给外网
        new HttpChannel(clientInputStream, serverOutputStream, latch).start();
        latch.await(120, TimeUnit.SECONDS);
        IOUtils.close(serverInputStream, serverOutputStream, server, clientInputStream, clientOutputStream, client);
    }

}
