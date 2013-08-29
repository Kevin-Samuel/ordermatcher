/*
 * Blitz Trading
 */
package ordermatcher.agents;

//import feederclient.controller.FeederClient;
//import feederclient.controller.mina.FeederEventListener;
//import feederclient.event.ChangeAskEvent;
//import feederclient.event.ChangeBidEvent;
//import feederclient.event.Event;
//import feederclient.event.NewAskEvent;
//import feederclient.event.NewBidEvent;
//import feederclient.event.SessionInfoEvent;
//import feederclient.event.SnapshotEvent;
//import feederclient.event.TradeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ordermatcher.controller.OrderFixAcceptor;
import ordermatcher.controller.OrderMatcher;
import ordermatcher.domain.AgentSettings;
import ordermatcher.domain.Order;
import org.bson.BasicBSONObject;
import org.slf4j.LoggerFactory;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.Side;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class MarketValueOrder implements Agent {
    
    // properties
    private String host = "localhost";
    private int port = 6755;
    
    // subcriptions list.
    //private static Map<String, SecuritySubscription> subcriptions = Collections.synchronizedMap(new HashMap<String, SecuritySubscription>());
    
    // Logger
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(MarketValueOrder.class);    
    

    @Override
    public void onNewOrder(Order order, OrderFixAcceptor matcher, SessionID session, AgentSettings settings) {
        
        if(settings.host!=null && !settings.host.isEmpty()) {
            host = settings.host;
        }
        
        if(settings.port > 0) {
            port = settings.port;
        }
        
        // try to retrieve the feeder subcription for the order security.
//        SecuritySubscription ss = subcriptions.get(order.security);
//        
//        if(ss == null) {
//            ss = new SecuritySubscription(matcher, session, order.security);
//            new Thread(ss).start();
//            subcriptions.put(order.security, ss);
//        }       
    }
    
    
//    private class SecuritySubscription implements FeederEventListener, Runnable {
//        
//        private String security;
//        private String exchange;
//        
//        private FeederClient client;
//        
//        private double askPrice = -1;
//        private double askQty = -1;
//        private double bidPrice = -1;
//        private double bidQty = -1;
//        
//        private SessionID session;
//        
//        private OrderFixAcceptor matcher;
//        
//        private boolean running = true;
//        
//        // Logger
//        private org.slf4j.Logger logger = LoggerFactory.getLogger(SecuritySubscription.class);
//        
//        public SecuritySubscription(OrderFixAcceptor matcher, SessionID session, String security) {            
//            this.matcher = matcher;
//            this.session = session;
//            this.security = security;
//            this.exchange = OrderMatcher.securitiesMarkets.get(security).equalsIgnoreCase("BOV") ? "XBSP" : "XBMF";
//        }
//
//        @Override
//        public synchronized void onResponse(Event evt) {
//            
//            Map<String, Object> params = evt.getEventParams();
//            
//            if(evt instanceof SessionInfoEvent) {
//                return;
//            }
//            
//            if(evt instanceof TradeEvent) {
//                askPrice = bidPrice = (Double) params.get("price");
//                askQty = bidQty = (Double) params.get("qty");
//                checkQueues();
//                return;
//            }
//            
//            if(evt instanceof NewBidEvent) {
//                
//                // retrieve price and qty and set internal properties
//                bidPrice = (Double) params.get("price");
//                bidQty = (Double) params.get("qty");
//                checkQueues();
//                return;
//            }
//            
//            if(evt instanceof NewAskEvent) {
//                
//                // retrieve price and qty and set internal properties
//                askPrice = (Double) params.get("price");
//                askQty = (Double) params.get("qty");                
//                checkQueues();
//                return;
//            }
//            
//            if(evt instanceof ChangeBidEvent) {
//                
//                // retrieve price and qty and set internal properties
//                bidPrice = (Double) params.get("price");
//                bidQty = (Double) params.get("qty");                
//                checkQueues();
//                return;
//            }
//            
//            if(evt instanceof ChangeAskEvent) {
//                
//                // retrieve price and qty and set internal properties
//                askPrice = (Double) params.get("price");
//                askQty = (Double) params.get("qty");                
//                checkQueues();
//                return;
//            }
//            
//            
//            if(evt instanceof SnapshotEvent) {
//
//               // retrieve bid and ask lists
//               List<BasicBSONObject> bidList = (List<BasicBSONObject>) params.get("bidList");
//               List<BasicBSONObject> askList = (List<BasicBSONObject>) params.get("askList");
//
//               for (BasicBSONObject bid : bidList) {
//                   
//                   bidPrice = (Double) bid.get("price");
//                   bidQty = (Double) bid.get("qty");                                   
//               }
//
//               for (BasicBSONObject ask : askList) {
//                   askPrice = (Double) ask.get("price");
//                   askQty = (Double) ask.get("qty");                                   
//               }
//               
//               checkQueues();               
//            }
//        }
//
//        @Override
//        public void run() {
//            
//            client = new FeederClient("OrderMatcherAgent:" + security + ":" + UUID.randomUUID().toString(), this);
//            client.connect(host, port);
//            client.subscribe(exchange, security, 1, false);
//            
//            while(running) {
//                try {
//                    synchronized(this) {
//                        this.wait();
//                    }
//                } 
//                catch (InterruptedException ex) {
//                    logger.error(ex.getMessage());
//                }
//            }
//        }
//        
//        public void terminate() {
//            
//            if(client!=null && client.isConnected()) {
//                client.unsubscribe(exchange, security, 1, false);
//                client.close();
//                client = null;
//            }
//            
//            running = false;
//            
//            synchronized(this) {
//                this.notifyAll();
//            }
//            
//            subcriptions.remove(security);
//        }
//
//        private void checkQueues() {
//            
//            boolean findBuyOrder = false;
//            boolean findSellOrder = false;
//            
//            try {
//                // check all buying orders
//                for(Order orderToBuy : matcher.toBuy) {
//                    
//                    if(orderToBuy.security.equals(security)) {
//                        findBuyOrder = true;
//                        checkOrder(orderToBuy, matcher, session);                        
//                    }
//                }                                                
//
//                // check all selling orders
//                for(Order orderToSell : matcher.toSell) {
//                    if(orderToSell.security.equals(security)) {
//                        findSellOrder = true;
//                        checkOrder(orderToSell, matcher, session);
//                    }
//                }
//                
//                if( !findBuyOrder && !findSellOrder) {
//                    this.terminate();
//                }
//
//                // match queues.
//                matcher.matchQueues(session);
//            } 
//            catch (SessionNotFound | CloneNotSupportedException ex) {
//                logger.error("Error matching queues. Terminating thread: " + ex.getMessage());
//                this.terminate();
//            }           
//        }
//        
//        private synchronized void checkOrder(Order order, OrderFixAcceptor matcher, SessionID session) throws CloneNotSupportedException {
//
//            if(order.session == null) {
//                return;
//            }
//
//            if(order.side == Side.BUY) {  
//                
//                logger.info("Buy order: " + order.security + ":" + order.price + ": Best sell-> " + askPrice);
//                if(order.price >= askPrice) {
//
//                    // put an inverse order                                    
//                    Order newOrder = (Order) order.clone();
//                    newOrder.session = null;
//                    newOrder.side = Side.SELL;
//                    newOrder.qty = askQty;
//                    matcher.toSell.add(newOrder);                    
//                }
//            }        
//            else {                                    
//
//                logger.info("Sell order: " + order.security + ":" + order.price + ": Best bid-> " + bidPrice);
//
//                if(order.price <= bidPrice) {
//
//                    // put an inverse order                
//                    Order newOrder = (Order) order.clone();
//                    newOrder.session = null;
//                    newOrder.side = Side.BUY;
//                    newOrder.qty = bidQty;
//                    matcher.toBuy.add(newOrder);
//                }
//            }
//        }
//    }
}