/*
 * Blitz Trading
 * 
 * @project 
 * @date 
 */

package ordermatcher.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo
 */
@XmlRootElement(name="MarketDataAcceptor")
public class MarketDataFixAcceptorSettings {
    
    @XmlAttribute(name="ConfigFile") 
    public String configFile;   
}
