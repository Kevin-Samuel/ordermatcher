/**
 * Blitz Trading
 * 
 * @project OrderMatcher 
 */
package ordermatcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import ordermatcher.controller.OrderMatcher;
import org.tanukisoftware.wrapper.WrapperListener;

/**
 * Console and Windows service launcher.
 * 
 * @author Sylvio Azevedo
 * @date 14/03/2013 
 */
public class Launcher implements WrapperListener {

    private static OrderMatcher server;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            server = new OrderMatcher();
            server.start();
        }
        catch (Exception ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Integer start(String[] strings) {
        
        try {
            server = new OrderMatcher();
            server.start();
        }
        catch (Exception ex) {
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        
        return 0;
    }

    @Override
    public int stop(int i) {
        
        // stop server.
        server.stop();        
        return 0;
    }

    @Override
    public void controlEvent(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
