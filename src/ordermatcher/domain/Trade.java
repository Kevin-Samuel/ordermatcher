/*
 * Blitz Trading
 * 
 * @project OrderMatcher
 * @date 23/09/2013
 */

package ordermatcher.domain;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Sylvio Azevedo
 */
public class Trade {
    
    // properties    
    public Date tradeTime;
    
    public Order buyerOrder;
    public Order sellOrder;
    
    public String security;
    public double price;
    public double qty;
    
    /**
     * Class representation.
     * 
     * @return String representing the Trade object.
     */
    @Override
    public String toString(){        
        
        // create a date formatter
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss.SSS");        
        
        // return the string representation of the Trade.
        return "[" + formatter.format(tradeTime) + "] " + security + " : " + qty + " : " + price;
    }
}
