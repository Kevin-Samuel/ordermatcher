/*
 * Blitz Trading
 */
package ordermatcher.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
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
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.AllocAccount;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.CxlRejResponseTo;
import quickfix.field.ExecID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LastShares;
import quickfix.field.LeavesQty;
import quickfix.field.NoAllocs;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class OrderFixAcceptor extends MessageCracker implements Application {
    
    // Logger
    private static Logger logger = LoggerFactory.getLogger(OrderFixAcceptor.class);
    
    // server settings
    private Settings settings;
    
    // properties
    public List<Order> toBuy = Collections.synchronizedList(new ArrayList<Order>());
    public List<Order> toSell = Collections.synchronizedList(new ArrayList<Order>());
    public List<Order> trades = Collections.synchronizedList(new ArrayList<Order>());
    
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
        // @todo deal better with this listenner later.s
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
            
                
            switch(order.side) {
                case Side.BUY:                    
                    logger.info("New order to BUY received - OrderId: " + order.orderId);
                    toBuy.add(order);
                    break;

                case Side.SELL:
                    logger.info("New order to SELL received - OrderId: " + order.orderId);                    
                    toSell.add(order);
                    break;
            }
            
            // send new order status.
            report42(clOrdId.getValue(), order, session, ExecType.NEW);            
            
            // match buy and sell queues.
            matchQueues(session);            
            
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
                    
                    agentObj.onNewOrder(order, this, session, agentSetting);
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
            
            // Find and remove order in the queues
            Order order = null;
            
            switch(side.getValue()) {
                
                case Side.BUY:
                     
                    order = find(origClOrdId.getValue(), toBuy);
                     
                     if(order!=null) {
                        logger.info("Order [" + order.orderId + ":BUY] will be removed.");                        
                        toBuy.remove(order);
                     }                    
                    break;

                case Side.SELL:
                    order = find(origClOrdId.getValue(), toSell);
                    
                    if(order!=null) {
                        logger.info("Order [" + order.orderId + ":SELL] will be removed.");                        
                        toSell.remove(order);
                     }                  
                    break;
            }
            
            // Check order retrieval and removal
            if(order == null) {
                
                // Send reject message
                reject(clOrdId.getValue(), clOrdId.getValue(), origClOrdId.getValue(), side.getValue(), "Can't find order with ID: [" + clOrdId.getValue() + "]", session);
                return;
            }
            
            order.orderId = clOrdId.getValue();
            
            // Report removal        
            order.status = OrdStatus.CANCELED;
            
            report42(clOrdId.getValue(), order, session, ExecType.CANCELED);
        
        } catch (FieldNotFound ex) {
            java.util.logging.Logger.getLogger(OrderFixAcceptor.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch(SessionNotFound snfe) {
            
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
                    
            Order order = side.getValue() == Side.BUY ? find(origClOrdId.getValue(), toBuy) : find(origClOrdId.getValue(), toSell);

            // Check order retrieval and removal
            if (order == null) {

                // Send reject message
                reject(clOrdId.getValue(), clOrdId.getValue(), origClOrdId.getValue(), side.getValue(), "Can't find order with ID: [" + clOrdId.getValue() + "]", session);
                return;
            }

            order.qty = qty.getValue();
            order.leavesQty = qty.getValue();
            order.price = price.getValue();
            order.status = OrdStatus.REPLACED;
            order.orderId = clOrdId.getValue();
            report42(clOrdId.getValue(), order, session, ExecType.REPLACE);
                
        } 
        catch (FieldNotFound ex) {
            java.util.logging.Logger.getLogger(OrderFixAcceptor.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (SessionNotFound snfe) {
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
            order.orderId = UUID.randomUUID().toString();
            order.security = security.getValue();
            order.price = price.getValue();
            order.qty = qty.getValue();
            order.leavesQty = qty.getValue();
            order.cumQty = 0;
            order.session = session;
            order.status = OrdStatus.NEW;
            order.side = side.getValue();
            order.fix = "FIX44";        

            synchronized(this) {

                switch(side.getValue()) {
                    case Side.BUY:   
                        logger.info("New order to BUY received - OrderId: " + order.orderId);
                        toBuy.add(order);
                        break;

                    case Side.SELL:
                        logger.info("New order to SELL received - OrderId: " + order.orderId);
                        toSell.add(order);
                        break;
                }        
            }

            report(clOrdId.getValue(), order, session, ExecType.NEW);

            matchQueues(session);
        
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
                        
                    agentObj.onNewOrder(order, this, session, agentSetting);                    
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

            // Find and remove order in the queues
            Order order = null;

            switch(side.getValue()) {

                case Side.BUY:                

                    order = find(orderId.getValue(), toBuy);

                    if(order != null) {
                        logger.info("Order [" + order.orderId + ":BUY] will be removed");

                        synchronized(this) {
                            toBuy.remove(order);
                        } 
                    }
                    break;

                case Side.SELL:                

                    order = find(orderId.getValue(), toSell);

                    if(order != null) {                    
                        logger.info("Order [" + order.orderId + ":SELL] will be removed");

                        synchronized(this) {
                            toSell.remove(order);
                        }
                    }
                    break;
            }

            // Check order retrieval and removal
            if(order == null) {            
                // Send reject message
                reject(orderId.getValue(), clOrdId.getValue(), origClOrdId.getValue(), side.getValue(), "Can't find order with ID: [${orderId.getValue()}]", session);
                return;
            }

            order.origClOrdId = clOrdId.getValue();

            // Report removal        
            order.status = OrdStatus.CANCELED;

            report(clOrdId.getValue(), order, session, ExecType.CANCELED);
                
        } 
        catch (FieldNotFound ex) {
            java.util.logging.Logger.getLogger(OrderFixAcceptor.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (SessionNotFound snfe) {
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

            // Find and remove order in the queues
            Order order = side.getValue() == Side.BUY ? 
                                      find(orderId.getValue(), toBuy) :
                                      find(orderId.getValue(), toSell);

            // Check order retrieval and removal
            if(order == null) {

                // Send reject message
                reject(orderId.getValue(), clOrdId.getValue(), origClOrdId.getValue(), side.getValue(), "Can't find order with ID: [${orderId.getValue()}]", session);
                return;
            }

            order.qty = qty.getValue();
            order.leavesQty = qty.getValue();
            order.price = price.getValue();
            order.status = OrdStatus.REPLACED;
            order.origClOrdId = clOrdId.getValue();

            // Report removal
            report(clOrdId.getValue(), order, session, ExecType.REPLACE);
                
        } 
        catch (FieldNotFound ex) {
            java.util.logging.Logger.getLogger(OrderFixAcceptor.class.getName()).log(Level.SEVERE, null, ex);
        } 
        catch (SessionNotFound snfe) {
        }
    }
    
    private void report(String clOrdId, Order order, SessionID session, char execType) throws SessionNotFound {
        
        // Send acceptation response
        quickfix.fix44.ExecutionReport newResp = new quickfix.fix44.ExecutionReport(            
            new OrderID(order.orderId),
            new ExecID(UUID.randomUUID().toString()),
            new ExecType(execType),
            new OrdStatus(order.status),
            new Side(order.side),
            new LeavesQty(order.leavesQty),
            new CumQty(order.cumQty),
            new AvgPx(0)
        );
        
        newResp.set(new ClOrdID(clOrdId));
        newResp.set(new OrigClOrdID(order.origClOrdId));
        newResp.set(new Symbol(order.security));
        
        if(execType == ExecType.TRADE) {
            newResp.set(new LastPx(order.lastPx));
            newResp.set(new LastQty(order.lastQty));
        }
        
        if(order.session!=null) {
            Session.sendToTarget(newResp, session);
        }
    }
    
    private void report42(String clOrdId, Order order, SessionID session, char execType) throws SessionNotFound {
        
        // Send acceptation response
        quickfix.fix42.ExecutionReport newResp = new quickfix.fix42.ExecutionReport(
            new OrderID(order.orderId),
            new ExecID(UUID.randomUUID().toString()),            
            new ExecTransType(ExecTransType.NEW),
            new ExecType(execType),
            new OrdStatus(order.status),
            new Symbol(order.security),
            new Side(order.side),
            new LeavesQty(order.leavesQty),
            new CumQty(order.cumQty),
            new AvgPx(0)
        );
        
        newResp.set(new ClOrdID(clOrdId));
        
        if(order.origClOrdId != null){
            newResp.set(new OrigClOrdID(order.origClOrdId));      
        }
        
        if(execType == ExecType.FILL || execType == ExecType.PARTIAL_FILL) {
            newResp.set(new LastPx(order.lastPx));
            newResp.set(new LastShares(order.lastQty));
        }
        
        if(order.session!=null) {
            Session.sendToTarget(newResp, session);
        }
    }
    
    public synchronized void matchQueues(SessionID session) throws SessionNotFound {
        
        // sort queues
        while(true) {
            
            /**
             * check if buy or sell queues are empty, if one is, leave, cause there
             * is nothing to match.
             */
           if(toBuy.isEmpty() || toSell.isEmpty()) {
               return;
           }
            
            // sort queues
            Collections.sort(toBuy);
            Collections.sort(toSell);
            
            // get max offer in buy queue
            Order maxBid = toBuy.get(toBuy.size()-1);
            
            // get min ask in sell queue
            Order minAsk = toSell.get(0);
            
            if(maxBid==null || minAsk==null) {
                break;
            }
            
            // if queue top orders don't match, break the loop.
            if(minAsk.price > maxBid.price) {
                break;                
            }
            
            double qty = maxBid.leavesQty >= minAsk.leavesQty ? minAsk.leavesQty : maxBid.leavesQty;
                
            maxBid.leavesQty = maxBid.leavesQty - qty;
            minAsk.leavesQty = minAsk.leavesQty - qty;           
            
            if(maxBid.leavesQty == 0) {
                logger.info("Bid FILLED - Bid queue size: " + toBuy.size());
                maxBid.status = OrdStatus.FILLED;               
                
                if(!toBuy.remove(maxBid)){
                    logger.error("Unable to remove the bid order");
                }
            }
            else {
                maxBid.status = OrdStatus.PARTIALLY_FILLED;
            }
            
            if(minAsk.leavesQty == 0) {
                logger.info("Ask FILLED - Ask queue size: " + toSell.size());
                minAsk.status = OrdStatus.FILLED;
                
                if(!toSell.remove(minAsk)) {
                    logger.error("Unable to remove the ask order");
                }
            }
            else{
                minAsk.status = OrdStatus.PARTIALLY_FILLED;
            }
            
            maxBid.lastQty = minAsk.lastQty = qty;
            maxBid.lastPx  = minAsk.lastPx = minAsk.price;
            
            maxBid.cumQty += qty;
            minAsk.cumQty += qty;
            
            if(maxBid.fix.equals("FIX44"))
            {
                report(maxBid.origClOrdId, maxBid, session, ExecType.TRADE);
                report(minAsk.origClOrdId, minAsk, session, ExecType.TRADE);
            }
            else
            {
                if(maxBid.status == OrdStatus.PARTIALLY_FILLED) {
                    report42(maxBid.orderId, maxBid, session, ExecType.PARTIAL_FILL);
                }
                else {
                    report42(maxBid.orderId, maxBid, session, ExecType.FILL);
                }
                    
                if(minAsk.status == OrdStatus.PARTIALLY_FILLED) {
                    report42(minAsk.orderId, minAsk, session, ExecType.PARTIAL_FILL);
                }
                else {
                    report42(minAsk.orderId, minAsk, session, ExecType.FILL);
                }
            }
        }
    }
    
    private Order find(String orderId, List<Order> orderList) {
    
        for(Order curr: orderList) {
            if(curr.orderId.equals(orderId)) {
                return curr;
            }
        }
        
        return null;
    }
    
    private void reject(String orderId, String clOrderId, String origClOrderId, char side, String msg, SessionID session) throws SessionNotFound {
        
        // Create a reject message
        // Send acceptation response
        quickfix.fix44.OrderCancelReject rejectResp = new quickfix.fix44.OrderCancelReject(            
            new OrderID(orderId),
            new ClOrdID(clOrderId),
            new OrigClOrdID(origClOrderId),            
            new OrdStatus(OrdStatus.REJECTED),
            new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST)
        );
        
        rejectResp.set( new Text(msg) );
        
        if(session!=null) {
            Session.sendToTarget(rejectResp, session);      
        }
    }
    
    private void reject42(String orderId, String clOrderId, String origClOrderId, char side, String msg, SessionID session) throws SessionNotFound {
        
        // Create a reject message
        // Send acceptation response
        quickfix.fix42.OrderCancelReject rejectResp = new quickfix.fix42.OrderCancelReject(
            new OrderID(orderId),
            new ClOrdID(clOrderId),
            new OrigClOrdID(origClOrderId),            
            new OrdStatus(OrdStatus.REJECTED),
            new CxlRejResponseTo(CxlRejResponseTo.ORDER_CANCEL_REQUEST)
        );
        
        rejectResp.set( new Text(msg) );
        
        if(session!=null) {
            Session.sendToTarget(rejectResp, session);      
        }
    }
}
