package com.jasonwjones.hyperion.thriller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;

import com.jasonwjones.jolo.ResultSetTablePrinter;

public class ThrillerTestSuite {

	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		// TODO Auto-generated method stub

		String embeddedSampleBasic = "jdbc:essbase:thriller://10.0.0.212/Sample.Basic;app=Sample;cube=Basic";
		Class.forName("com.jasonwjones.hyperion.thriller.ThrillerDriver");

		File testQueryFile = new File("src/test/resources/result-test-01.mdx");
		String testQuery = FileUtils.readFileToString(testQueryFile, Charset.defaultCharset());
	
		Connection conn = DriverManager.getConnection(embeddedSampleBasic, "admin", "password");
		Statement statement = conn.createStatement();
		ResultSet resultSet = statement.executeQuery(testQuery);
		
		new ResultSetTablePrinter().printResultSet(resultSet);
		
	}

}
