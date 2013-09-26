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
@XmlRootElement(name="Agent")
public class AgentSettings {

    @XmlAttribute(name="Name")
    public String name;
    
    @XmlAttribute(name="Implementation")
    public String impl;
    
    @XmlAttribute(name="Host")
    public String host;
    
    @XmlAttribute(name="Port")
    public int port;
    
    @XmlAttribute(name="Active")
    public boolean active;
}

