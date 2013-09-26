/*
 * Blitz Trading
 */
package ordermatcher.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
@XmlRootElement(name="FixAcceptor")
public class FixAcceptorSettings {
    
    @XmlAttribute(name="ConfigFile") 
    public String configFile;
}
