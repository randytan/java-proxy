
package me.randytan.proxy.helper;

import me.randytan.proxy.Main;
import me.randytan.proxy.model.SystemProp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Utilities {

    static final Logger logger = LogManager.getLogger(Utilities.class);

    private Utilities(){}

    public static void loadConfiguration(){
        Properties prop = new Properties();
        InputStream input = null;
        try{
            input = Main.class.getClassLoader().getResourceAsStream("system.properties");
            prop.load(input);

            SystemProp systemProp = new SystemProp();
            systemProp.setBindPort(Integer.parseInt(prop.getProperty("proxy.bindAddr")));
            systemProp.setProxyAddress(prop.getProperty("proxy.hostAddr"));
            systemProp.setProxyPort(prop.getProperty("proxy.portAddr"));

        } catch(IOException ioe) {
            logger.error(ioe.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

    }

}
