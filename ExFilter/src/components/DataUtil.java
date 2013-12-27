package components;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.os.Environment;

public class DataUtil {
	private static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd[HH:mm:ss]", Locale.CANADA);
	private static String fname = df.format(new Date()) + ".txt";
	private static StringBuilder data = new StringBuilder();
	private static int strokeCount = 0;
	
	public static void reset(){
		df = new SimpleDateFormat("yyyy-MM-dd[HH:mm:ss]", Locale.CANADA);
		fname = df.format(new Date()) + ".txt";
		data = new StringBuilder();
		strokeCount = 0;
	}
	
	//set master header with the experiment info
	public static void masterHeader(String id){
		data.append("USER:"+id+"\n");
	}
	
	//write the dimension of the screen into the data file once
	public static void writeDim(int width, int height){
		data.append(Integer.toString(width)+","+Integer.toString(height)+"\n");
	}
	
	//writes the head info for each part of the experiment
	public static void writeHead(String name, String type){
		data.append("h:"+name+":"+type+"\n");
	}
	
	//writes the ending of a part, that is the control data
	public static void writeEnd(String coords){
		data.append("e:"+coords+"\n");
	}
	
	//for no control filter
	public static void writeEmptyEnd(){
		data.append("e:0\n");
	}
	
	//writes in a '-' separator for each part of an experiment
	public static void endPart(){
		data.append("-\n");
	}
	
	//appends a new stroke on a new line given only coordinates and timestamps and a bool indicating validity
	public static void writeStroke(String stroke, Boolean valid, Boolean control, long birthOffset){
		String head = Integer.toString(strokeCount);
		strokeCount++;
		head += (valid) ? "[1]" : ((control) ? "[2]" : "[0]");
		head += Long.toString(birthOffset);
		data.append(head+stroke+"\n");
	}
	
	//TODO provide ender function to write in the control data
	
	//Appends current buffer to the file if it exists and closes the file, then clears the buffer
	//Usually use at the end of a particular drawing
	public static void dataCommit(){
		File file = new File(Environment.getExternalStorageDirectory(), fname);
		try {
			if(!file.exists()){
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file,true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(data.toString());
			bw.close();
			data = new StringBuilder();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
