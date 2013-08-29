/*
 * Blitz Trading
 */
package ordermatcher.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import ordermatcher.domain.Settings;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class SettingsController {
 
    public static Settings load(String configFilePath) throws JAXBException, FileNotFoundException {
        
        JAXBContext ctx = JAXBContext.newInstance(Settings.class);
        Unmarshaller u = ctx.createUnmarshaller();
    
        return (Settings) u.unmarshal(new FileInputStream(configFilePath));
    }
    
    public static void save(Settings settings, String configFilePath) throws JAXBException, FileNotFoundException {
        
        JAXBContext ctx = JAXBContext.newInstance(Settings.class);        
        Marshaller m = ctx.createMarshaller();
        
        m.marshal(m, new FileOutputStream(configFilePath));        
    }
}
