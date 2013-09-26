/*
 * Blitz Trading
 */
package ordermatcher.domain;

import org.bson.BasicBSONObject;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class SecurityMarket extends BasicBSONObject {
    
    // properties
    public String security;
    public String exchange;    
    
    public void pack() {
        security = (String) this.get("ticker");
        exchange = (String) this.get("exchange");
    }
}
