/*
 * Blitz Trading
 */
package ordermatcher.controller;

import ordermatcher.agents.Agent;
import ordermatcher.domain.AgentSettings;
import ordermatcher.domain.Order;
import ordermatcher.domain.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.AllocAccount;
import quickfix.field.ClOrdID;
import quickfix.field.NoAllocs;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class OrderFixAcceptor extends MessageCracker implements Application {
    
    // Logger
    private static Logger logger = LoggerFactory.getLogger(OrderFixAcceptor.class);
    
    // server settings
    private Settings settings;
    
    public OrderFixAcceptor(Settings settings) {        
        this.settings = settings;
    }

    @Override
    public void onCreate(SessionID session) {
        logger.info("Creating session: " + session.toString());
    }

    @Override
    public void onLogon(SessionID session) {
        logger.info("FIX session [" + session.toString() + "] established.");
    }

    @Override
    public void onLogout(SessionID session) {
        logger.info("FIX session [" + session.toString() + "] terminated.");
    }

    @Override
    public void toAdmin(Message msg, SessionID session) {
        // @todo deal better with this listenner later.
        logger.info("Admin message to client: " + msg.toString());
    }

    @Override
    public void fromAdmin(Message msg, SessionID session) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.info("Admin message received: " + msg.toString());
    }

    @Override
    public void toApp(Message msg, SessionID session) throws DoNotSend {
        // @todo deal better with this listenner later.
        logger.info("Message to client: " + msg.toString());
    }

    @Override
    public void fromApp(Message msg, SessionID session) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        
        try{
            crack(msg, session);
        }
        catch(Exception e) {
            logger.error("Error cracking fix messsage: " + e.getMessage());
        }
    }
    
    /**
     * FIX 42 Messages
     * =========================================================================
     */        
    public void onMessage(quickfix.fix42.NewOrderSingle message, SessionID session) {
        
        try {
            /** Retrieve Client Order Identification */
            ClOrdID clOrdId = new ClOrdID();
            message.get(clOrdId);
            
            /** Get order information */
            Account account = new Account();
            message.get(account);            
            
            Symbol security = new Symbol();
            message.get(security);            
            
            Price price = new Price();
            message.get(price);            
            
            OrderQty qty = new OrderQty();
            message.get(qty);            
            
            Side side = new Side();
            message.get(side);
            
            /** Create a new order instance */
            Order order = new Order();
            order.origClOrdId = null;
            order.orderId = clOrdId.getValue();
            order.security = security.getValue();
            order.price = price.getValue();
            order.qty = qty.getValue();
            order.leavesQty = qty.getValue();
            order.cumQty = 0;
            order.session = session;
            order.status = OrdStatus.NEW;
            order.side = side.getValue();
            order.fix = "FIX42";            
                
            OrderMatcher.bookManager.newOrder(order);
            
            // start order agents
            for(AgentSettings agentSetting : settings.agents) {
                
                if(agentSetting.active) {
                    
                    Agent agentObj = (Agent) Class.forName(agentSetting.impl).newInstance();
                    
                    if(agentObj == null) {
                        return;                        
                    }
                    
                    logger.info("Invoking agent [" + agentSetting.name + "]");
                    
                    if(agentSetting.host != null) {
                        logger.info("Agent client of host [" + agentSetting.host + "]");
                    }
                    
                    if(agentSetting.port != 0) {
                        logger.info("Agent client using port [" + agentSetting.port + "]");                        
                    }
                    
                    agentObj.onNewOrder(order, OrderMatcher.bookManager, session, agentSetting);
                }
            }
        }
        catch (FieldNotFound ex) {
            logger.error("Field could not be found: " + ex.getMessage());
        } 
        catch (SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
        catch (Exception e) {
            logger.error("Agent could not be instanced: " + e.getMessage());
        }
    }
    
    public void onMessage(quickfix.fix42.OrderCancelRequest message, SessionID session) {
        
        try {
            // Client order identification
            ClOrdID clOrdId = new ClOrdID();
            message.get(clOrdId);
            
            // Orign client order identification
            OrigClOrdID origClOrdId = new OrigClOrdID();
            message.get(origClOrdId);
            
            // Side
            Side side = new Side();
            message.get(side);        
            
            // Security
            Symbol security = new Symbol();
            message.get(security);
            
            Order order = new Order();
            order.orderId = origClOrdId.getValue();
            order.fix = "FIX42";
            order.session = session;
            order.side = side.getValue();            
            order.security = security.getValue();
            
            OrderMatcher.bookManager.cancelOrder(order);
        
        } 
        catch (FieldNotFound ex) {
            logger.error("Error retrieving field: " + ex.getMessage());
        }
        catch(SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
    }
    
    public void onMessage(quickfix.fix42.OrderCancelReplaceRequest message, SessionID session) {
        
        try {
            // Client order identification
            ClOrdID clOrdId = new ClOrdID();            
            message.get(clOrdId);
                    
            OrigClOrdID origClOrdId = new OrigClOrdID();
            message.get(origClOrdId);
            
            OrderQty qty = new OrderQty();
            message.get(qty); 
            
            Price price = new Price();
            message.get(price);
        
            Side side = new Side();
            message.get(side);
            
            // Security
            Symbol security = new Symbol();
            message.get(security);
                    
            Order order = new Order();
            order.orderId = clOrdId.getValue();
            order.origClOrdId = origClOrdId.getValue();
            order.qty = qty.getValue();
            order.price = price.getValue();
            order.side = side.getValue();
            order.fix = "FIX42";
            order.session = session;
            order.security = security.getValue();
            
            OrderMatcher.bookManager.modifyOrder(order);
        } 
        catch (FieldNotFound ex) {
            logger.error("Error retrieving field: " + ex.getMessage());
        } 
        catch (SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
    }
    
    /**
     * FIX 44 Messages
     * =========================================================================
     */        
    public void onMessage(quickfix.fix44.NewOrderSingle message, SessionID session) {
        
        try {
            
            ClOrdID clOrdId = new ClOrdID();
            message.get(clOrdId);

            NoAllocs noAllocs = new NoAllocs();
            message.get(noAllocs);

            quickfix.fix44.NewOrderSingle.NoAllocs group = new quickfix.fix44.NewOrderSingle.NoAllocs();
            message.getGroup(1, group);

            AllocAccount account = new AllocAccount();
            group.get(account);

            Symbol security = new Symbol();
            message.get(security);

            Price price   = new Price();
            message.get(price);

            OrderQty qty = new OrderQty();
            message.get(qty);

            Side side    = new Side();
            message.get(side);

            Order order = new Order();

            order.origClOrdId = clOrdId.getValue();
            //order.orderId = UUID.randomUUID().toString();
            order.orderId = clOrdId.getValue();
            order.security = security.getValue();
            order.price = price.getValue();
            order.qty = qty.getValue();
            order.leavesQty = qty.getValue();
            order.cumQty = 0;
            order.session = session;
            order.status = OrdStatus.NEW;
            order.side = side.getValue();
            order.fix = "FIX44";        

            OrderMatcher.bookManager.newOrder(order);
            
            // start order agents
            for(AgentSettings agentSetting : settings.agents) {
                
                if(agentSetting.active) {
                    
                    Agent agentObj = (Agent) Class.forName(agentSetting.impl).newInstance();
                    
                    if(agentObj == null) {
                        return;                        
                    }
                    
                    logger.info("Invoking agent [" + agentSetting.name + "]");
                    
                    if(agentSetting.host != null) {
                        logger.info("Agent client of host [" + agentSetting.host + "]");
                    }
                    
                    if(agentSetting.port != 0) {
                        logger.info("Agent client using port [" + agentSetting.port + "]");                        
                    }
                        
                    agentObj.onNewOrder(order, OrderMatcher.bookManager, session, agentSetting);                    
                }
            }
        } 
        catch (FieldNotFound ex) {
            logger.error("Field could not be found: " + ex.getMessage());
        } 
        catch (SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
        catch (Exception e) {
            logger.error("Agent could not be instanced: " + e.getMessage());
        }        
    }
    
    public void onMessage(quickfix.fix44.OrderCancelRequest message, SessionID session) {
     
        try {
            // Client order identification
            ClOrdID clOrdId = new ClOrdID();
            message.get(clOrdId);

            // Orign client order identification
            OrigClOrdID origClOrdId = new OrigClOrdID();
            message.get(origClOrdId);

            // Order identification (market side)
            OrderID orderId = new OrderID();
            if(message.isSet(orderId)) {
                message.get(orderId);
            }

            // Side
            Side side = new Side();
            message.get(side);
            
            // Symbol
            Symbol security = new Symbol();
            message.get(security);

            // Find and remove order in the queues
            Order order = new Order();
            order.orderId = clOrdId.getValue();
            order.origClOrdId = origClOrdId.getValue();
            order.side = side.getValue();
            order.security = security.getValue();
                    
            order.fix = "FIX44";
            order.session = session;

            OrderMatcher.bookManager.cancelOrder(order);                
        } 
        catch (FieldNotFound ex) {
            logger.error("Error retrieving field: " + ex.getMessage());
        } 
        catch (SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
    }
    
    public void onMessage(quickfix.fix44.OrderCancelReplaceRequest message, SessionID session) {
        
        try {
    
            // Client order identification
            ClOrdID clOrdId = new ClOrdID();
            message.get(clOrdId);

            // Orign client order identification
            OrigClOrdID origClOrdId = new OrigClOrdID();
            message.get(origClOrdId);

            // Order identification (market side)
            OrderID orderId = new OrderID();
            if(message.isSet(orderId)) {
                message.get(orderId);
            }

            // Retrieve quantity
            OrderQty qty = new OrderQty();
            message.get(qty);

            // Retrieve price
            Price price = new Price();
            message.get(price);

            // Side
            Side side = new Side();
            message.get(side);
            
            // Symbol
            Symbol security = new Symbol();
            message.get(security);

            Order order = new Order();
            order.orderId = clOrdId.getValue();
            order.origClOrdId = origClOrdId.getValue();
            order.qty = qty.getValue();
            order.price = price.getValue();
            order.side = side.getValue();
            order.security = security.getValue();
            
            order.fix = "FIX44";
            order.session = session;
            
            OrderMatcher.bookManager.modifyOrder(order);
        } 
        catch (FieldNotFound ex) {
            logger.error("Error retrieving field: " + ex.getMessage());
        } 
        catch (SessionNotFound snfe) {
            logger.error("Session not found to send messages: " + snfe.getMessage());
        }
    }
}