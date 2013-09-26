/*
 * Bliz Trading
 */
package ordermatcher.agents;

import java.util.Random;
import ordermatcher.controller.BookManager;
import ordermatcher.controller.OrderFixAcceptor;
import ordermatcher.domain.AgentSettings;
import ordermatcher.domain.Book;
import ordermatcher.domain.Order;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.Side;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class InverseOrder implements Agent, Runnable {
    
    // properties
    private Order order;
    private Book book;
    private BookManager bookManager;
    private SessionID session;
    private AgentSettings settings;
    
    // Logger
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(OrderFixAcceptor.class);

    @Override
    public void onNewOrder(Order order, BookManager bookManager, SessionID session, AgentSettings settings) {
     
        this.order = order;        
        this.bookManager = bookManager;
        this.session = session;
        this.settings = settings;
        
        this.book = bookManager.books.get(order.security);
        
        new Thread(this).start();
    }

    @Override
    public void run() {
        
        try {
            Thread.sleep((new Random().nextInt(10)*1000) + 1000);
        } 
        catch (InterruptedException ex) {
            logger.error("Problem when slepping: " + ex.getMessage());
            return;
        }
                
        // put an inverse order
        Order newOrder;
        try {
            newOrder = (Order) order.clone();
        } catch (CloneNotSupportedException ex) {
            logger.error("Problem cloning order: " + ex.getMessage());
            return;
        }
        
        newOrder.session = null;

        // check if order side is BUY
        if(order.side == Side.BUY) {                    
            newOrder.side = Side.SELL; 
            book.toSell.add(newOrder);
        }
        else {                    
            // put an inverse order                    
            newOrder.side = Side.BUY;
            book.toBuy.add(newOrder);
        }
        try {
            bookManager.match(book);
        } catch (SessionNotFound ex) {
            logger.error("Session not found to send new order message: " + ex.getMessage());
        }
    }
}