/*
 * Blitz Trading
 */
package ordermatcher.mina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import ordermatcher.bson.BsonCommand;
import ordermatcher.controller.OrderMatcher;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.bson.BasicBSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class BsonHandler extends IoHandlerAdapter {
    
    // logger
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    // constants
    public static final String BSON_CMD_PACKAGE = "feederserver.bson.command.";
    
    // mutex
    public static final Object mutex = new Object();    
    
    public BsonHandler() {
    }
   
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception
    {
        logger.error(cause.getMessage());
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception
    {   
        if(!(message instanceof BasicBSONObject)) {        
        
            List args = new ArrayList();
            args.add("-100");
            args.add("Command is not valid bson object.");            
            
            BasicBSONObject bson = new BasicBSONObject();
            bson.put("Handler", "Message");        
            bson.put("Args", args);
            
            return;
        }
     
        // retrieve bson object.
        BasicBSONObject bsonObj = (BasicBSONObject) message;
        
        // retrieve bson command name
        String cmdName = bsonObj.getString("Name");
        
        // retrieve command arguments
        List args = (ArrayList) bsonObj.get("Args");
        
        logger.info("Command received [" + cmdName + "]. Processing...");
        
        // instance and execute bson command by reflection.
        BsonCommand cmd = (BsonCommand) Class.forName(BSON_CMD_PACKAGE + cmdName).newInstance();        
        cmd.setHandler(this);        
        cmd.execute(args, session);
    }

    @Override
    public void sessionIdle( IoSession session, IdleStatus status ) throws Exception
    {
        System.out.println( "IDLE " + session.getIdleCount( status ));
    }
    
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        
        logger.info("Session open. Sending server and session identification.");
        
        String sessionId = UUID.randomUUID().toString();
        
        session.setAttribute("sessionId", sessionId);
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("id", sessionId);
        args.put("name", OrderMatcher.settings.name);
        
        BasicBSONObject bson = new BasicBSONObject();
        bson.put("Handler", "SessionInfoEvent");
        bson.put("Args", args);     
        
        // send server info to client;
        session.write(bson);
    }
}
