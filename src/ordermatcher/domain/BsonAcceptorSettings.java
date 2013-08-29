/*
 * Blitz Trading
 */
package ordermatcher.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sylvio Azevedo
 */
@XmlRootElement(name = "BsonAcceptor")
public class BsonAcceptorSettings {
 
    @XmlAttribute(name="Port")
    public int port;
}
