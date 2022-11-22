package tech.ynfy.module.proxy.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * 〈流工具类〉
 *
 * @author Hsiong
 * @version 1.0.0
 * @since 2022/11/22
 */
public class IOUtils {
    /**
     * 关闭所有流
     */
    public static void close(Closeable... closeables) {
        if (closeables != null) {
            for (int i = 0; i < closeables.length; i++) {
                if (closeables[i] != null) {
                    try {
                        closeables[i].close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
