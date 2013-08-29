/*
 * Blitz Trading 
 */
package ordermatcher.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
@XmlRootElement(name="Database")
public class DatabaseSettings {
    
    @XmlAttribute(name="ConnString")
    public String connString;
    
    @XmlAttribute(name="DatabaseName")
    public String databaseName;
    
    @XmlAttribute(name="Name")
    public String name;
}
