package testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import finalProject.Filter;

/**
 * The class that records sensor readings in a text file.
 */
public class SensorRecorder extends Thread{
	
	private Filter[] filters;
	private final int PERIOD;
	private PrintWriter p;
	private boolean isRunning;
	
	/**
	 * Constructor for sensorRecorder.
	 * 
	 * @param filters The filtered sensor values that are to be recorded.
	 * @param period The time interval to record at, in ms.
	 * @param fileName The name of the text file to save the data to.
	 */
	public SensorRecorder(Filter[] filters, int period, String fileName){
		this.filters = filters;
		this.PERIOD = period;
		this.isRunning = true;
		File f = new File(fileName + ".txt");
		try{this.p = new PrintWriter(new FileOutputStream(f));}catch(Exception e){}
	}
	
	//required for thread
	public void run(){
		
		long start, end;
		String s;
		
		for(int i=0; isRunning; i++){
			start = System.currentTimeMillis();
			s = "" + i*PERIOD;
			for(Filter f : filters){
				f.addSample();
				s += " " + f.getFilteredData();
			}
			p.println(s);

			end = System.currentTimeMillis();
			if (end - start < PERIOD) {
				try {
					Thread.sleep(PERIOD - (end - start));
				} catch (InterruptedException e) {
					// nothing to do here
				}
			}
		}
	}
	
	/**
	 * Stops the sensor recording and saves it in the text file.
	 */
	public void stopRecording(){
		isRunning = false;
		p.close();
	}
}
