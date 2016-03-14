package testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import finalProject.Filter;

public class SensorRecorder extends Thread{
	
	private Filter[] filters;
	private final int PERIOD;
	private PrintWriter p;
	private boolean isRunning;
	
	public SensorRecorder(Filter[] filters, int period, String fileName){
		this.filters = filters;
		this.PERIOD = period;
		this.isRunning = true;
		File f = new File(fileName + ".txt");
		try{this.p = new PrintWriter(new FileOutputStream(f));}catch(Exception e){}
	}
	
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
	
	public void stopRecording(){
		isRunning = false;
		p.close();
	}
}
