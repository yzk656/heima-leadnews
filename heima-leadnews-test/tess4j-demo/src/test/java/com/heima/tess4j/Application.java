package com.heima.tess4j;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.File;

public class Application {

    /**
     * 识别图片中的文字
     * @param args
     */
    public static void main(String[] args) throws TesseractException {
        //创建实例
        Tesseract tesseract = new Tesseract();

        //设置字体库路径
        tesseract.setDatapath("E:\\Code\\springcloud_heima");

        //设置语言
        tesseract.setLanguage("chi_sim");

        File file = new File("C:\\Users\\27939\\Pictures\\heima\\123.png");

        //识别图片
        String s = tesseract.doOCR(file);
        System.out.println(s.replaceAll("\\r|\\n","-"));
    }
}
