/**
 * Blitz Trading
 *
 * @project Order matcher
 */
package ordermatcher.controller;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;
import java.net.UnknownHostException;
import ordermatcher.domain.DatabaseSettings;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo
 */
public class MongoDBController implements Database {

    // inner reference of database settings
    DatabaseSettings settings;
    
    // mongo database representation
    private Mongo mongo;
    private DB db;
    
    // logger
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(MongoDBController.class);

    @Override
    public boolean connect(DatabaseSettings settings) {

        try {
            // keep settings reference inside.
            this.settings = settings;

            mongo = new Mongo(new MongoURI(settings.connString));
            db = mongo.getDB(settings.databaseName);
        } 
        catch (Exception ex) {
            logger.error("It was not possible establish a database connection: " + ex.getMessage());
            return false;    
        }
        
        logger.info("[" + settings.name + "] Connection successfully established.");
        return true;
    }

    @Override
    public void close() {

        // check if there is a connection
        if (mongo == null) {
            return;
        }

        // close mongo cnonection.
        mongo.close();

        // clear references.
        mongo = null;
        db = null;
    }

    @Override
    public Object getConnection() {
        return db;
    }

    @Override
    public String getConnString() {
        return this.settings.connString;
    
    }

    @Override
    public String getDatabaseName() {
        return this.settings.databaseName;
    }

    @Override
    public String getName() {
        return this.settings.name;
    }
}