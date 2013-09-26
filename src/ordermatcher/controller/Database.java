/**
 * Blitz Trading
 *
 * @project StrategyServer
 * @date 11/07/2012
 */
package ordermatcher.controller;

import ordermatcher.domain.DatabaseSettings;

/**
 *
 * @author Sylvio Azevedo
 */
public interface Database {
    
    public boolean connect(DatabaseSettings settings);
    public void close();
    public Object getConnection();
    public String getConnString();
    public String getDatabaseName();
    public String getName();
}