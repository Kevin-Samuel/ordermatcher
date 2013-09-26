/*
 * Blitz Trading
 */
package ordermatcher.mina.codec;

import com.mongodb.DBEncoder;
import com.mongodb.DefaultDBEncoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.bson.BasicBSONObject;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.OutputBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class BsonEncoder implements ProtocolEncoder {
    
    private final Logger logger = LoggerFactory.getLogger(BsonEncoder.class);

    @Override
    public void encode(IoSession session, Object msg, ProtocolEncoderOutput out) throws Exception {
        
        logger.info("Write response object.");
         
        DBEncoder encoder = DefaultDBEncoder.FACTORY.create();
        
        OutputBuffer ob = new BasicOutputBuffer();                
        encoder.writeObject(ob, (BasicBSONObject) msg);
        
        ByteBuffer buffer = ByteBuffer.allocate(ob.size() + 4);
        buffer.putInt(ob.size());
        buffer.put(ob.toByteArray());
        buffer.flip();
        
        if(buffer!=null)
        {            
            out.write(buffer);
            out.flush();
        }
    }

    @Override
    public void dispose(IoSession session) throws Exception {
        // not thing to do.
    }
    
}
