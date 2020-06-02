package com.nowcoder.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "D:\\Java\\nowcoder_project\\wkhtmltopdf\\bin\\wkhtmltoimage --quality 75 https://www.nowcoder.com d:/Java/nowcoder_project/wk_image/2.png";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
