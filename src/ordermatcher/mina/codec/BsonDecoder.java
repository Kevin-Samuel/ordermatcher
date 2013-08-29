/*
 * Blitz Trading
 */
package ordermatcher.mina.codec;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBDecoder;
import com.mongodb.DefaultDBDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class BsonDecoder extends CumulativeProtocolDecoder {
    
    private final Logger logger = LoggerFactory.getLogger(BsonDecoder.class);

    @Override
    protected boolean doDecode(IoSession session, ByteBuffer in, ProtocolDecoderOutput out) throws Exception {
        
        if(!in.prefixedDataAvailable(4)) {
            return false;
        }
        
        /**
         * ATTENTION! Do not remove this line. It skips the 4 first bytes from
         * message header.
         */
        // int length = in.getInt();        
        in.skip(4);
        
        DBDecoder decoder = DefaultDBDecoder.FACTORY.create();
        
        BasicDBObject response = (BasicDBObject) decoder.decode(in.asInputStream(), (DBCollection) null);
        
        if(response == null) {        
            logger.error("Could not decode Bson document.");
            return false;
        }
        
        out.write(response);        
        return true;
    }
}
