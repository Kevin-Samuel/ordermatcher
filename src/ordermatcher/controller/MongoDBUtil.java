/*
 * Blitz Trading
 */
package ordermatcher.controller;

import com.mongodb.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.BSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sylvio Azevedo - sylvio.azevedo@blitz-trading.com
 */
public class MongoDBUtil {
    
    private DB db;
    
    public static final int ASC = 1;
    public static final int DESC = -1;
    
    // logger
    private final Logger logger = LoggerFactory.getLogger(MongoDBUtil.class);
    
    public MongoDBUtil(DB db) {
        this.db = db;
    }
    
    public void insert(String collectionName, Map<String, Object> values) {

        // Retrieve collection
        DBCollection collection = db.getCollection(collectionName);

        //
        BasicDBObject doc = new BasicDBObject();

        doc.putAll(values);

        collection.insert(doc);
        db.requestDone();
    }
    
    
    public void insert(Object toInsert) {
        
        // Retrieve collection
        DBCollection collection = db.getCollection(toInsert.getClass().getSimpleName());
        
        BasicDBObject doc = new BasicDBObject();
        
        doc.putAll((BSONObject) toInsert);

        collection.insert(doc);
        db.requestDone();
    }

    
    public List<Object> findAll(String collection, Map<String, Object> parameters) {
    
        return this.findAll(collection, collection, parameters);
    }
    
    public List<Object> findAll(String collection, String implClass, Map<String, Object> parameters) {
        
        DBCollection dbCollection = db.getCollection(collection);        
        
        DBObject doc = new BasicDBObject();        
        
        if(parameters!=null) {
            doc.putAll(parameters);
        }
        
        DBCursor cursor = dbCollection.find(doc);
        
        List<Object> list = new ArrayList<Object>();        
        
        while(cursor.hasNext()) {
            
            doc = cursor.next();
            
            try {                
                BSONObject bson = (BSONObject) Class.forName(implClass).newInstance();                
                bson.putAll(doc);
                
                list.add(bson);
            } 
            catch (Exception ex) {
                logger.error("An object [" + collection + "] could not be retrieved. Instance: " + doc.toString());
            }
        }
        
        return list;
    }
    
    public Object find(String collection, Map<String, Object> parameters) {
        try {
            DBCollection dbCollection = db.getCollection(collection);
            
            DBObject doc = new BasicDBObject();        
            doc.putAll(parameters);        
            
            DBCursor cursor = dbCollection.find(doc);
            
            if(!cursor.hasNext()) {
                return null;
            }
            
            doc = cursor.next();
            BSONObject bson = (BSONObject) Class.forName("strategy.domain." + collection).newInstance();                
            bson.putAll(doc);
            
            return bson;
        } 
        catch (Exception ex) {
            logger.error("It was not proceed with the search: " + ex.getMessage());
            return null;
        }
    }
    
    public Object find(String collection, String implClass, Map<String, Object> parameters) {
        try {
            DBCollection dbCollection = db.getCollection(collection);
            
            DBObject doc = new BasicDBObject();        
            doc.putAll(parameters);        
            
            DBCursor cursor = dbCollection.find(doc);
            
            if(!cursor.hasNext()) {
                return null;
            }
            
            doc = cursor.next();
            BSONObject bson = (BSONObject) Class.forName(implClass).newInstance();                
            bson.putAll(doc);
            
            return bson;
        } 
        catch (Exception ex) {
            logger.error("It was not proceed with the search: " + ex.getMessage());
            return null;
        }
    }
    
    public Object findLast(String collection, String implClass, Map<String, Object> parameters, String sortField, int direction) {
        try {
            DBCollection dbCollection = db.getCollection(collection);
            
            DBObject doc = new BasicDBObject();        
            doc.putAll(parameters);        
            
            DBCursor cursor = dbCollection.find(doc).sort(new BasicDBObject(sortField, direction));
            
            if(!cursor.hasNext()){
                return null;
            }
            
            doc = cursor.next();
            BSONObject bson = (BSONObject) Class.forName(implClass).newInstance();                
            bson.putAll(doc);
            
            return bson;
        } 
        catch (Exception ex) {
            logger.error("It was not proceed with the search: " + ex.getMessage());
            return null;
        }
    }
    
    public void remove(Object object) {
        
        DBCollection collection = db.getCollection(object.getClass().getSimpleName());
        
        BasicDBObject doc = new BasicDBObject();        
        doc.putAll((BSONObject) object);

        collection.findAndRemove(doc);
        
        db.requestDone();
    }
    
    public void remove(String collection, Map<String, Object> parameters) {
        
        DBCollection dbCollection = db.getCollection(collection);
        
        BasicDBObject doc = new BasicDBObject();        
        doc.putAll(parameters);

        dbCollection.findAndRemove(doc);
        
        db.requestDone();
        
    }
    
    public void update(Object object) {
        
        DBCollection collection = db.getCollection(object.getClass().getSimpleName());
        
        BasicDBObject param = new BasicDBObject();
        param.putAll((BSONObject) object);
        
        BasicDBObject query = new BasicDBObject();
        query.put("id", param.get("id"));        

        collection.findAndModify(query, param);
        
        db.requestDone();
    }
    
    public void update(String collection, Map<String, Object> parameters) {
        
        DBCollection dbCollection = db.getCollection(collection);
        
        BasicDBObject param = new BasicDBObject();
        param.putAll(parameters);
        
        BasicDBObject query = new BasicDBObject();
        query.put("id", param.get("id"));        

        dbCollection.findAndModify(query, param);
        
        db.requestDone();
    }
}
