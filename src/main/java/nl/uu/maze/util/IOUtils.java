package nl.uu.maze.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class IOUtils {
	
	public static void saveTxtToFile(String fname, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
	    writer.write(content) ; 
	    writer.close();
	}
}
