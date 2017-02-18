package com.mzherdev.jdkbase;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Created by mikhail on 17.02.17.
 */
public class App {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.println("Working...");

        BaseClass baseClass = null;
        long sum = 0;
        try {
            baseClass = new BaseClass();
            baseClass.setN(1_000_000);

            baseClass.insertData();
            baseClass.createXmlOne();
            Path path = baseClass.createXmlTwo();
            sum = baseClass.parseXml(path);
            if(baseClass.getConnection() != null)
                baseClass.getConnection().close();
        } catch (IOException | SAXException | SQLException | ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long workTime = (endTime - startTime) / 1000;

        System.out.println("Sum is " + sum + ", time is " + workTime + "s");
    }
}
