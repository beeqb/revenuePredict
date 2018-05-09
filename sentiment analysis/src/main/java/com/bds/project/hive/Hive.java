package com.bds.project.hive;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger; 

public class Hive {
	private static String driverName = "org.apache.hive.jdbc.HiveDriver";
	private static String url = "jdbc:hive2://localhost:10000/default";
	private static String user = "root";
	private static String password = "cloudera";
	private static String sql = "";
	private static ResultSet res;
	private static final Logger log = Logger.getLogger(Hive.class); 
	
	public Connection getConnection() throws Exception {
		Class.forName(driverName);
		Connection con = DriverManager.getConnection(url, user, password);;

		return con;
	}

	public Statement setUpSimpleTable(Connection con,String tableName, String dataFilePath,String sql) throws Exception {
		Statement stmt = null;

		stmt = con.createStatement();

		// drop table. ignore error.
		stmt.execute("drop table " + tableName);

		// create table
		stmt.execute("create table "
						+ tableName
						+ " "+sql+" ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' STORED AS TEXTFILE");

		// load data
		stmt.execute("load data local inpath '"
				+ dataFilePath.toString() + "' into table " + tableName);
		return stmt;
	}

	protected Statement setUpPartitionTable(Connection con,String tableName, String dataFilePath, String parsql) throws Exception {
		Statement stmt = null;

		stmt = con.createStatement();

		stmt.execute("drop table " + tableName);
		stmt.execute("create table " + tableName
				+ " " + parsql);
		
		
		stmt.execute("load data local inpath '"
				+ dataFilePath.toString() + "' into table "
				+ tableName + " PARTITION (date)");
		return stmt;
	}


	protected static void printResultSet(ResultSet res) throws Exception {
		if (res == null)
			return;
		res.next();
		System.out.println(res.getString(1));
		res.next();
		System.out.println(res.getString(1));
		//while (res.next()) {
			//System.out.println(res.getFetchSize()); // + " " + res.getString(2));
		//}
	}
	
	public static void main(String[] args) {
	
	}

}
