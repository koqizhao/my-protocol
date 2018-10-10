package io.mine.protocol.data;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public class Util {

    public static String multiply(String data, int times) {
        if (times <= 1)
            return data;

        return data + multiply(data, times - 1);
    }

}
