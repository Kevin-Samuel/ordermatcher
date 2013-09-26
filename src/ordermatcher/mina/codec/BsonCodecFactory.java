/*
 * Blitz Trading
 */
package ordermatcher.mina.codec;


import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class BsonCodecFactory implements ProtocolCodecFactory{
    
    // properties
    private BsonEncoder encoder;
    private BsonDecoder decoder;    
    
    public BsonCodecFactory() {
        encoder = new BsonEncoder();
        decoder = new BsonDecoder();
    }
    
    @Override
    public ProtocolEncoder getEncoder() throws Exception {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder() throws Exception {
        return decoder;
    }
    
}
