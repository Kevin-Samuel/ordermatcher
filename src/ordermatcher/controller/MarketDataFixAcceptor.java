/*
 * Blitz Trading
 * 
 * @project 
 * @date 
 */

package ordermatcher.controller;

import java.util.logging.Level;
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
import quickfix.UnsupportedMessageType;
import quickfix.field.SubscriptionRequestType;

/**
 *
 * @author Sylvio Azevedo
 */
public class MarketDataFixAcceptor  extends MessageCracker implements Application {
    
    private static Logger logger = LoggerFactory.getLogger(OrderFixAcceptor.class);
    
    // server settings
    private Settings settings;
    
    
    public MarketDataFixAcceptor(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void onCreate(SessionID session) {
        // Logger    
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
    
    public void onMessage(quickfix.fix44.MarketDataRequest msg, SessionID session) {
        
        
        try {            
            // retrieve subscription request type
            SubscriptionRequestType subscriptionRequestType = new SubscriptionRequestType();                
            msg.get(subscriptionRequestType);
            
            switch(subscriptionRequestType.getValue()) {
                
                case SubscriptionRequestType.SNAPSHOT:
                    break;
                    
                case SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES:
                    break;
            }
        }
        catch (FieldNotFound ex) {
            logger.error("Field has not been set in the message: " + ex.getMessage());
        }
    }
}
