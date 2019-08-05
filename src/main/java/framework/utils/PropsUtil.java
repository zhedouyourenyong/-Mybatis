package framework.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public final class PropsUtil
{

    public static Properties loadProps (String fileName)
    {
        Properties props = null;
        InputStream is = null;
        try
        {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
            if(is == null)
            {
                throw new FileNotFoundException(fileName + " file is not found");
            }
            props = new Properties();
            props.load(is);
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            if(is != null)
            {
                try
                {
                    is.close();
                } catch (IOException e)
                {
                  e.printStackTrace();
                }
            }
        }
        return props;
    }
}
