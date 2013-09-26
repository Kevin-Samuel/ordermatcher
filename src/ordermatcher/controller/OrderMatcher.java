/*
 * Blitz Trading
 */
package ordermatcher.controller;

import com.mongodb.DB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import ordermatcher.domain.SecurityMarket;
import ordermatcher.domain.Settings;
import ordermatcher.mina.BsonHandler;
import ordermatcher.mina.codec.BsonCodecFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Acceptor;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

/**
 *
 * @author Sylvio Azevedo <sylvio.azevedo@blitz-trading.com>
 */
public class OrderMatcher implements Runnable {
    
    // private constants
    private static final String VERSION = "1.0";
    private static final String CONFIG_FILE = "etc"  + File.separator + "OrderMatcher.xml";
    
    // properties
    private boolean running;
    private Thread server;        
    private IoAcceptor bsonAcceptor;    
    private Acceptor acceptor;   
    
    // book manager handler.
    public static BookManager bookManager = new BookManager();
    
    // static properties
    public static Map<String, String> securitiesMarkets = new HashMap<String, String>();
    
    // server settings
    public static Settings settings;
    
    // logger
    private static Logger logger = LoggerFactory.getLogger(OrderMatcher.class.getName());        

    public void start() throws JAXBException, FileNotFoundException {
        
        // set running flag.
        running = true;
        
        // start server thread.
        (server = new Thread(this)).start();
    }

    public void stop() {
        
        // stop admin bson acceptor
        bsonAcceptor.unbindAll();        
        
        // stop fix acceptor
        acceptor.stop();        
        
        running = false;
        
        synchronized(this) {            
            this.notify();
        }
    }
    
    
    public void startAdminAcceptor() throws IOException {
        
        logger.info("Starting BSON acceptor.");
        
        bsonAcceptor = new org.apache.mina.transport.socket.nio.SocketAcceptor();
        
        bsonAcceptor.getFilterChain().addLast("logging", new LoggingFilter());
        bsonAcceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new BsonCodecFactory()));                
        bsonAcceptor.bind(new InetSocketAddress(settings.bsonAcceptor.port), new BsonHandler());
        
        logger.info("Bson acceptor is up and listening on port: " + settings.bsonAcceptor.port);
        
    }
    
    public void startOrderFixAcceptor() throws FileNotFoundException, ConfigError {
        
        OrderFixAcceptor oma = new OrderFixAcceptor(settings);        
        SessionSettings sessionSettings = new SessionSettings(new FileInputStream(settings.fixAcceptor.configFile));
        
        MessageStoreFactory stf = new FileStoreFactory(sessionSettings);
        LogFactory lf = new FileLogFactory(sessionSettings);
        MessageFactory mf = new DefaultMessageFactory();
        
        acceptor = new SocketAcceptor(oma, stf, sessionSettings, lf, mf);
                
        acceptor.start();        
    }
    
    public void startMarketDataFixAcceptor() throws FileNotFoundException, ConfigError {
        
        MarketDataFixAcceptor mda = new MarketDataFixAcceptor(settings);        
        SessionSettings sessionSettings = new SessionSettings(new FileInputStream(settings.marketDataFixAcceptor.configFile));
        
        MessageStoreFactory stf = new FileStoreFactory(sessionSettings);
        LogFactory lf = new FileLogFactory(sessionSettings);
        MessageFactory mf = new DefaultMessageFactory();
        
        acceptor = new SocketAcceptor(mda, stf, sessionSettings, lf, mf);
                
        acceptor.start();        
    }

    public void loadData() {
        
        // connect to AAS database.
        MongoDBController mongoCtr = new MongoDBController();
        mongoCtr.connect(settings.database);
        
        // Load ticker symbols.
        MongoDBUtil mongoUtil = new MongoDBUtil((DB) mongoCtr.getConnection());        
        List<Object> list = mongoUtil.findAll("Security", "ordermatcher.domain.SecurityMarket", null);
        
        for(Object curr: list) {            
            SecurityMarket security = (SecurityMarket) curr;
            
            security.pack();
            
            if(security.exchange == null) {
                continue;
            }
            
            securitiesMarkets.put(security.security, security.exchange);
        }
    }
    
    @Override
    public void run() {
        
        // set location of log4j configuration file
        PropertyConfigurator.configure("etc/log4j.properties");
        
        logger.info("Feeder Server - Version: " + VERSION);
        
        // read server configuration settings
        try {
            logger.info("Reading configuration settings file.");
            settings = SettingsController.load(CONFIG_FILE);                               
        }
        catch (Exception ex) {
            logger.error("Can not read configuration file [" + CONFIG_FILE + "]. Aborting...");
            System.exit(-1);
        }
        
        // load database information.
        //loadData();
        
        // start administration bson acceptor.
        try {                        
            startAdminAcceptor();
        } 
        catch (IOException ex) {
            logger.error("Error starting BSON acceptor, last error: " + ex.getMessage());
            System.exit(-1);
        }
        
        // start order fix acceptor.
        try {
            startOrderFixAcceptor();
        }
        catch (Exception ex) {
            logger.error("Error starting order FIX acceptor, last error: " + ex.getMessage());
            System.exit(-1);
        }
        
        // start market data fix acceptor.
        try {
            startMarketDataFixAcceptor();
        }
        catch (Exception ex) {
            logger.error("Error starting Maket Data acceptor, last error: " + ex.getMessage());
            System.exit(-1);
        }
        
        while(running) {
            synchronized(this) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {
                    logger.error("Interruption exception: " + ex.getMessage());
                }
            }
        }
    }
}
