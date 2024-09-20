
package com.timesheetapp.util;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DatabaseUtils {


    /**
     * Constructs new bare IniEditor instance.
     */
    public DatabaseUtils() {
    }


    public Connection createConnection() throws Exception {
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        IniEditor cnfgFile = new IniEditor();
        cnfgFile.load("c:/data/ini/tandb.ini");
        //cnfgFile.load("/frc/ini/frcmg.ini");
        String host = cnfgFile.get("HostDatabase", "HOST");
        String port = cnfgFile.get("HostDatabase", "PORT");
        String dsn = cnfgFile.get("HostDatabase", "DSN");
        String uid = cnfgFile.get("HostDatabase", "UID");
        String pwd = cnfgFile.get("HostDatabase", "PWD");
        String conn = "jdbc:db2://" + host + ":" + port + "/" + dsn;
        return DriverManager.getConnection(conn, uid, pwd);
    }
    /**
     * Create connection to database.
     *
     * @param conn the connect string "jdbc:db2://host:port/db";
     * @param user the user's name to connect DB
     * @param pwd the password to connect DB
     * @return connection object or null when can't connect to DB
     *
     */
    public Connection createConnection(String conn, String user, String pwd) throws Exception {
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        return DriverManager.getConnection(conn, user, pwd);
    }

}
