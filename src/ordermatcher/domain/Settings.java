/*
 * Blitz Trading
 */
package ordermatcher.domain;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
@XmlRootElement(name = "OrderMatcher")
public class Settings {
    
    @XmlElement(name="Name")
    public String   name;        
    
    @XmlElement(name="BsonAcceptor")
    public BsonAcceptorSettings bsonAcceptor;
    
    @XmlElement(name="FixAcceptor")
    public FixAcceptorSettings fixAcceptor;
    
    @XmlElement(name="Database")
    public DatabaseSettings database;
    
    @XmlElementWrapper(name = "Agents")
    @XmlElement(name="Agent")
    public List<AgentSettings> agents;
}


