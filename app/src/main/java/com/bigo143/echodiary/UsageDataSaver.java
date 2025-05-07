package com.bigo143.echodiary;

import android.content.Context;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class UsageDataSaver {

    public static void saveUsageToXml(Context context, Map<String, Long> usageMap) {
        try {
            String fileName = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + ".xml";
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);

            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, "UTF-8");
            serializer.startDocument("UTF-8", true);
            serializer.startTag(null, "usage");

            for (Map.Entry<String, Long> entry : usageMap.entrySet()) {
                serializer.startTag(null, "app");

                serializer.attribute(null, "name", entry.getKey());
                serializer.attribute(null, "duration", String.valueOf(entry.getValue()));

                serializer.endTag(null, "app");
            }

            serializer.endTag(null, "usage");
            serializer.endDocument();
            serializer.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
