/* Blitz Trading
 */
package ordermatcher.agents;

import ordermatcher.controller.BookManager;
import ordermatcher.domain.AgentSettings;
import ordermatcher.domain.Order;
import quickfix.SessionID;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public interface Agent {
    public void onNewOrder(Order order, BookManager bookManager, SessionID session, AgentSettings settings);    
}