package uk.co.omgdrv.simplevgm.util;
// http://www.slack.net/~ant/

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DataReader {

    static InputStream openGZIP(InputStream in) throws Exception
    {
        return new GZIPInputStream(in);
    }


    // Loads entire stream into byte array
    static byte[] loadData(InputStream in) throws Exception
    {
        byte[] data = new byte[256 * 1024];
        int size = 0;
        int count;
        while ((count = in.read(data, size, data.length - size)) != -1)
        {
            size += count;
            if (size >= data.length)
                data = Util.resize(data, data.length * 2);
        }

        if (data.length - size > data.length / 4)
            data = Util.resize(data, size);

        return data;
    }
}
