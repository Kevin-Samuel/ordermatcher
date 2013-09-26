/*
 * Blitz Trading
 * 
 * @project OrderMatcher
 * @date 20/09/2013
 */

package ordermatcher.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ordermatcher.domain.Book;
import ordermatcher.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
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
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Text;

/**
 *
 * @author Sylvio Azevedo
 */
public class BookManager {
    
    // Logger
    private static Logger logger = LoggerFactory.getLogger(OrderFixAcceptor.class);

    // list of managed books
    public Map<String, Book> books = new HashMap<String, Book>();
    
    public synchronized void newOrder(Order order) throws SessionNotFound {
        
        Book book;
        
        // check if exists a book for the order security
        if(!books.containsKey(order.security.toUpperCase())) {            
            book = new Book();
            books.put(order.security.toUpperCase(), book);
        }
        else {
            book = books.get(order.security.toUpperCase());
        }
        
        // check order side and insert it in the right list.
        if(order.side == Side.BUY) {
            
            // insert order in the buy list            
            book.toBuy.add(order);         
            
            // sort list by price
            Collections.sort(book.toBuy);
        }
        else {
            
            // inser order in the sell list
            book.toSell.add(order);

            // sort list by price
            Collections.sort(book.toSell);
        }
        
        // Report as new order
        if(order.fix.equals("FIX44")) {
            report(order.orderId, order, order.session, ExecType.NEW);
        }
        else {
            report42(order.orderId, order, order.session, ExecType.NEW);
        }
        
        match(book);
    }
    
    public void modifyOrder(Order order) throws SessionNotFound {
        
        Book book = books.get(order.security.toUpperCase());
        
        if(book == null) {
            logger.error("Book not fount for: " + order.security.toUpperCase());
            return;
        }
        
        // Find and remove order in the queues
        Order toUpdate = order.side == Side.BUY ? 
                                  find(order.origClOrdId, book.toBuy) :
                                  find(order.origClOrdId, book.toSell);

        // Check order retrieval and removal
        if(toUpdate == null) {

            // Send reject message
            reject(order.orderId, order.orderId, order.origClOrdId, "Can't find order with ID: [${orderId.getValue()}]", order.session);
            return;
        }

        toUpdate.qty = order.qty;
        toUpdate.leavesQty = order.qty;
        toUpdate.price = order.price;
        toUpdate.status = OrdStatus.REPLACED;
        toUpdate.orderId = order.orderId;
        toUpdate.origClOrdId = order.origClOrdId;

        // Report removal
        if(order.fix.equals("FIX44")) {
            report(order.orderId, order, order.session, ExecType.REPLACE);
        }
        else {
            report42(order.orderId, order, order.session, ExecType.REPLACE);
        }
    }
    
    public void cancelOrder(Order order) throws SessionNotFound {
        
        Book book = books.get(order.security.toUpperCase());
        
        if(book == null) {            
            logger.error("Book not fount for: " + order.security.toUpperCase());
            return;
        }
        
        Order toCancel = null;
        
        // Find and remove order in the queues
        switch(order.side) {

            case Side.BUY:                

                toCancel = find(order.origClOrdId, book.toBuy);

                if(toCancel != null) {
                    logger.info("Order [" + toCancel.orderId + ":BUY] will be removed");

                    synchronized(this) {
                        book.toBuy.remove(toCancel);
                    } 
                }
                break;

            case Side.SELL:                

                toCancel = find(order.origClOrdId, book.toSell);

                if(toCancel != null) {                    
                    logger.info("Order [" + toCancel.orderId + ":SELL] will be removed");

                    synchronized(this) {
                        book.toSell.remove(toCancel);
                    }
                }
                break;
        }

        // Check order retrieval and removal
        if(toCancel == null) {
            
            // Send reject message
            reject(order.orderId, order.orderId, order.origClOrdId, "Can't find order with ID: [${orderId.getValue()}]", order.session);
            return;
        }

        toCancel.origClOrdId = order.orderId;

        // Report removal        
        toCancel.status = OrdStatus.CANCELED;

        if(order.fix.equals("FIX44")) {
            report(order.orderId, order, order.session, ExecType.CANCELED);
        }
        else {
            report42(order.orderId, order, order.session, ExecType.CANCELED);
        }
    }

    public void match(Book book) throws SessionNotFound {
        
        // sort queues
        while(true) {
            
            /**
             * check if buy or sell queues are empty, if one is, leave, cause there
             * is nothing to match.
             */
           if(book.toBuy.isEmpty() || book.toSell.isEmpty()) {
               return;
           }            
            
            // get max offer in buy queue
            Order maxBid = book.toBuy.get(book.toBuy.size()-1);
            
            // get min ask in sell queue
            Order minAsk = book.toSell.get(0);
            
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
                logger.info("Bid FILLED - Bid queue size: " + book.toBuy.size());
                maxBid.status = OrdStatus.FILLED;               
                
                if(!book.toBuy.remove(maxBid)){
                    logger.error("Unable to remove the bid order");
                }
            }
            else {
                maxBid.status = OrdStatus.PARTIALLY_FILLED;
            }
            
            if(minAsk.leavesQty == 0) {
                logger.info("Ask FILLED - Ask queue size: " + book.toSell.size());
                minAsk.status = OrdStatus.FILLED;
                
                if(!book.toSell.remove(minAsk)) {
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
                report(maxBid.origClOrdId, maxBid, maxBid.session, ExecType.TRADE);
                report(minAsk.origClOrdId, minAsk, minAsk.session, ExecType.TRADE);
            }
            else
            {
                if(maxBid.status == OrdStatus.PARTIALLY_FILLED) {
                    report42(maxBid.orderId, maxBid, maxBid.session, ExecType.PARTIAL_FILL);
                }
                else {
                    report42(maxBid.orderId, maxBid, maxBid.session, ExecType.FILL);
                }
                    
                if(minAsk.status == OrdStatus.PARTIALLY_FILLED) {
                    report42(minAsk.orderId, minAsk, minAsk.session, ExecType.PARTIAL_FILL);
                }
                else {
                    report42(minAsk.orderId, minAsk, minAsk.session, ExecType.FILL);
                }
            }
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
    
    private Order find(String orderId, List<Order> orderList) {
    
        for(Order curr: orderList) {
            if(curr.orderId.equals(orderId)) {
                return curr;
            }
        }
        
        return null;
    }
    
    private void reject(String orderId, String clOrderId, String origClOrderId, String msg, SessionID session) throws SessionNotFound {
        
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
    
    private void reject42(String orderId, String clOrderId, String origClOrderId, String msg, SessionID session) throws SessionNotFound {
        
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
