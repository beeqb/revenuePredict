package com.bds.project.of;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.bds.project.hive.Hive;

import opin.config.Config;
import opin.entity.*;
import opin.logic.AnnotationHandler;
import opin.preprocessor.*;
import opin.supervised.ExpressionPolarityClassifier;
import opin.featurefinder.*;

public class OF {
	
	public static void create_tables(String product){
		Hive test = new Hive();
		System.out.println("Connecting to Hive....");
		Connection con;
		try {
			con = test.getConnection();
			System.out.println("Setting up table....");
			String tableName = product+"_reviews";
			String dataFilePath = "/home/cloudera/Work/input/"+tableName+".txt";
			String sql = "(id string, comment string, rating string, title string, author string, productName string, date string) ";
			Statement stmt = test.setUpSimpleTable(con, tableName, dataFilePath, sql);
			
			String parTableName = "par_"+tableName; 
			stmt.execute("create table "+parTableName+" (id string, comment string, rating string, title string, author string, productName string) partitioned by (date string)");
			String formatDate = null;
			String month = null;
			String rawDate = null;
			
			for(int y=2013; y<=2018; y++){
				for(int m=1; m<=12; m++){
					switch(m){
						case 1:
							month = "January";
							break;
						case 2:
							month = "February";
							break;
						case 3:
							month = "March";
							break;
						case 4:
							month = "April";
							break;
						case 5:
							month = "May";
							break;
						case 6:
							month = "June";
							break;
						case 7:
							month = "July";
							break;
						case 8:
							month = "August";
							break;
						case 9:
							month = "September";
							break;
						case 10:
							month = "October";
							break;
						case 11:
							month = "November";
							break;
						case 12:
							month = "December";
							break;
					}
					for(int d=1; d<=31; d++){
						if(m<10){
							formatDate = y+"-0"+m;
						}else{
							formatDate = y+"-"+m;
						}
						if(d<10){
							formatDate += "-0"+d;
						}else{
							formatDate += "-"+d;
						}
						System.out.println(formatDate);
						rawDate = "on "+month+" "+d+", "+y;
						sql = "FROM "+tableName+" r INSERT OVERWRITE TABLE "+parTableName+" PARTITION(date='"+formatDate+"') SELECT r.id, r.comment, r.rating, r.title, r.author, r.productName where date='\""+rawDate+"\"'";
						stmt.execute(sql);
					}
				}
			}
			
			con.close();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void writeFiles(String product){
		System.out.println("Starting writing to files...");
		Hive test = new Hive();
		System.out.println("Connecting to Hive....");
		try {
			Connection con = test.getConnection();
			Statement stmt = con.createStatement();
			
			String tableName = "par_"+product+"_reviews";
			String date = null;
			String infofile = "./input/"+product+"_info";
			String docfile = product+".doclist";
			FileOutputStream info_out = new FileOutputStream(infofile);
			FileOutputStream doclist_out = new FileOutputStream(docfile);
			info_out.write("date\tcount\taverageRating\r\n".getBytes());
			for(int y=2013; y<=2018; y++){
				for(int m=1; m<=12; m++){
					String month;
					if(m<10){
						month="0"+m;
					}else{
						month=Integer.toString(m);
					}
					for(int d=1; d<=31; d++){
						if(d<10){
							date = y+"-"+month+"-0"+d;
						}else{
							date = y+"-"+month+"-"+d;
						}
						String sql = "select comment, title, rating from "+tableName+" where date = '"+date+"'";
						ResultSet set = stmt.executeQuery(sql);
						if(set.next()){
								String filename = "./input/"+product+"/"+date;
								FileOutputStream out = new FileOutputStream(filename);
								int count = 1;
								float totalRating = 0;
								
								out.write(set.getString(1).getBytes());
								out.write("\t".getBytes());
								out.write(set.getString(2).getBytes());
								out.write("\r\n".getBytes());
						        float rating = Float.parseFloat(set.getString(3).split(" ")[0].substring(1, 3));
						        totalRating += rating;
								
								while(set.next()){
									count++;
									out.write(set.getString(1).getBytes());
									out.write("\t".getBytes());
									out.write(set.getString(2).getBytes());
									out.write("\r\n".getBytes());
							        rating = Float.parseFloat(set.getString(3).split(" ")[0].substring(1, 3));
							        totalRating += rating;
								}
								float averageRating = totalRating/count;
								info_out.write((date+"\t"+count+"\t"+averageRating+"\r\n").getBytes());
								doclist_out.write(("/home/cloudera/workspace/project/input/"+product+"/"+date+"\r\n").getBytes());
								out.close();
						}
					}
				}
			}
			info_out.close();
			doclist_out.close();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void generatePolarity(String product){
		Config conf = new Config();
		for(int i=1;i<=10;i++){
			String[] options = {product+".doclist"+i,"-d"}; 
	        if(!conf.parseCommandLineOptions(options)){
	        	System.exit(-1);
	        }
	        
	        /*
	         * Creating corpus object to process
	         */
	        Corpus corpus= new Corpus(conf);
	        
	        
	        /*
	         * Pre-processing the corpus
	         */
	        if(conf.isRunPreprocessor()){
	        	PreProcess preprocessor = new PreProcess(conf);
	            preprocessor.process(corpus);
	        }
	        
	       
	        /*
	         * Finding lexicon clues in the corpus
	         */
	        
	        if(conf.isRunClueFinder()){
	           ClueFind clueFinder = new ClueFind(conf);
	           clueFinder.process(corpus);
	        }
	        

	        /*
	         * Prepare data for classification 
	         */
	        
	        AnnotationHandler annHandler = new AnnotationHandler(conf);
	        if(conf.isRunRulebasedClassifier() || conf.isRunSubjClassifier() || conf.isRunPolarityClassifier()){
	        	annHandler.buildSentencesFromGateDefault(corpus);
	        }
	        
	        
	        /*
	         *  Applying polarity classifier to the corpus
	         */
	        
	        if(conf.isRunPolarityClassifier()){
	        	annHandler.readInRequiredAnnotationsForPolarityClassifier(corpus);
	        	ExpressionPolarityClassifier polarityClassifier = new ExpressionPolarityClassifier(conf);
	        	polarityClassifier.process(corpus);
	        }
		}
    	
        
	}
	
	public static int compute(String filename){
		int score = 0;
		InputStreamReader inputfile;
		try {
			inputfile = new InputStreamReader(new FileInputStream(filename));
			BufferedReader reader = new BufferedReader(inputfile);
			String line = reader.readLine();
			while(line != null){
				String polarity = (line).split("\t")[1];
				switch(polarity){
				case "neutral":
					break;
				case "negative":
					score--;
					break;
				case "positive":
					score++;
					break;
				}
				line = reader.readLine();
			}
			inputfile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return score;
		
	}
	
	public static void generateScore(String product){
		
		try {
			String writefile = "./input/"+product+"_sum";
			FileOutputStream out = new FileOutputStream(writefile);
			String readfile = "./input/"+product+"_info";
			InputStreamReader in = new InputStreamReader(new FileInputStream(readfile));
			BufferedReader read = new BufferedReader(in);
			String line = read.readLine();
			out.write((line+"\tcommentScore"+"\r\n").getBytes());
			line = read.readLine();
			while(line != null){
				String date = (line.split("\t"))[0];
				String file = "./input/"+product+"/"+date+"_auto_anns/exp_polarity.txt";
				String score = Integer.toString(compute(file));
				out.write((line+"\t"+score+"\r\n").getBytes());
				line = read.readLine();
			}
			in.close();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		
		//create partition tables
		String product = "adidas";
		//create_tables(product);
		//write comments each day to txt files
		//writeFiles(product);
		
		//generatePolarity(product);
		generateScore(product);
	}

}
