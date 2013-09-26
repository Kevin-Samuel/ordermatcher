/*
 * Blitz Trading
 * 
 * @project 
 * @date 
 */

package ordermatcher.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Sylvio Azevedo
 */
public class Book {
    
    // properties
    public List<Order> toBuy = Collections.synchronizedList(new ArrayList());    
    public List<Order> toSell = Collections.synchronizedList(new ArrayList());
    
    public Trade lastTrade;
}
