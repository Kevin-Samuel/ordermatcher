/*
 * Blitz Trading
 */
package ordermatcher.bson;

import java.util.List;
import ordermatcher.mina.BsonHandler;
import org.apache.mina.common.IoSession;


/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public interface BsonCommand {
    void setHandler(BsonHandler handler);
    void execute(List args, IoSession session);
}
