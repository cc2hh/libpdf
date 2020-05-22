package com.sclbxx.libpdf.util;

import android.os.Environment;
import android.util.Xml;

import com.sclbxx.libpdf.pojo.Login;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * function: xml解析 <p>
 * author: cc <p>
 * data: 2016/10/24 16:08 <p>
 * version: V1.0 <p/>
 */
public class XMLUtils {

    public static Login readBaseInfo() throws IOException, XmlPullParserException {

        String path = Environment.getExternalStorageDirectory() + "/workspace/user/" + "baseInfo.xml";
        //解析xml文件
        FileInputStream in;
        Login login = new Login();
            File file_cfg = new File(path);
            if (!file_cfg.exists()) {
                return login;
            }
            in = new FileInputStream(file_cfg);
            XmlPullParser parser = Xml.newPullParser();//得到Pull解析器
            parser.setInput(in, "UTF-8");//设置下输入流的编码
            int eventType = parser.getEventType();//得到第一个事件类型
            while (eventType != XmlPullParser.END_DOCUMENT) {//如果事件类型不是文档结束的话则不断处理事件
                switch (eventType) {
                    case (XmlPullParser.START_DOCUMENT)://如果是文档开始事件
//                        user = new User();//创建一个user
                        break;
                    case (XmlPullParser.START_TAG)://如果遇到标签开始
                        String tagName = parser.getName();// 获得解析器当前元素的名称
                        switch (tagName) {
                            case "baseHttp":
                                login.url = parser.nextText();
                                break;
                            case "studentAccount":
                                login.userAccount = parser.nextText();
                                break;
                            case "studentId":
                                login.studentId = parser.nextText();
                                break;
                            case "schoolId":
                                login.schoolId = parser.nextText();
                                break;
                            case "passpwd":
                                login.userPwd = parser.nextText();
                                break;
                            case "despassword":
                                login.despassword = parser.nextText();
                                break;
                        }
                        break;
                    case (XmlPullParser.END_TAG)://如果遇到标签结束
                        break;
                }
                eventType = parser.next();//进入下一个事件处理
            }
        return login;
    }
}
