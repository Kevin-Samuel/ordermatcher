/*
 * Blitz Trading
 */
package ordermatcher.domain;

import quickfix.SessionID;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class Order implements Comparable<Order>, Cloneable {
    
    // properties
    public String orderId;
    public String origClOrdId;
    
    public String security;
    public String account;    
    
    public double price;
    public double qty;
    public double cumQty;
    public double leavesQty;
    public double lastPx;
    public double lastQty;
    
    /**
     * FIX status
     * 
     * 0 = New 
     * 1 = Partially filled
     * 2 = Filled
     * 3 = Done for day
     * 4 = Canceled
     * 6 = Pending cancel
     * 7 = Stopped
     * 8 = Rejected
     * 9 = Suspended
     * A = Pending new
     * B = Calculated
     * C = Expired
     * D = Accepted for bidding
     * E = Pending replace
     */
    public char status;
    
    public String type;
    
    public SessionID session;
    
    public char    side;
    
    public String fix;
    
    @Override
    public String toString() {     
        
        StringBuilder sb = new StringBuilder(orderId).append(":")
                        .append(account).append(":")
                        .append(security).append(":")
                        .append("qty").append(":")
                        .append("price");
        
        return sb.toString();
    }

    @Override
    public int compareTo(Order o) {
        return this.price == o.price? 0 : (this.price < o.price? -1 : 1);
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {        
        return super.clone();        
    }
}
