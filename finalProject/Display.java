package finalProject;

import lejos.hardware.lcd.TextLCD;

/**
 * The class responsible for controlling the LCD display.
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 */
public class Display extends Thread{
	
	// member variables
	private final TextLCD t;
	private Object[] objects;
	private final int PERIOD = 250;
	private Object lock;
	
	/**
	 * Constructor for Display
	 * 
	 * @param t The TextLCD of the EV3
	 * @param objects The objects that will be displayed on the screen through the toString() method.
	 */
	public Display(TextLCD t, Object[] objects){
		this.t = t;
		this.objects = objects;
		lock = new Object();
	}
	
	// required for thread
	public void run(){
		
		long displayStart, displayEnd;
		int line;
		String s;
		t.clear();
		
		while(true){
			displayStart = System.currentTimeMillis();
			line = 0;
			
			synchronized (lock) {
				//display all the objects
				for(Object o : objects){
					s = o.toString();
					//parse the string every 16 chars because the display length is 16 chars
					for(int i=0; i < s.length(); i += 16){
						t.drawString(s.substring(i, Math.min(s.length(), i+16)), 0, line);
						line++;
					}
				}
			}
			
			//only display once per period
			displayEnd = System.currentTimeMillis();
			if (displayEnd - displayStart < PERIOD) {
				try {
					Thread.sleep(PERIOD - (displayEnd - displayStart));
				} catch (InterruptedException e) {
					// nothing to do here
				}
			}
		}
		
	}
	
	/**
	 * Changes the objects that will be displayed on the screen.
	 * 
	 * @param objects The objects that will be displayed on the screen through the toString() method.
	 */
	public void setObjects(Object[] objects){
		synchronized(lock){
			this.objects = objects;
		}
	}
}
