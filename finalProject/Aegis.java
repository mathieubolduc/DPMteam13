package finalProject;

import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The class responsible for controlling the Anti Enemy Goal and Interception System (AEGIS).
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 */
public class Aegis {
	
	//member variables
	private boolean open;
	private EV3LargeRegulatedMotor flapsMotor;
	
	/**
	 * Constructor for Aegis.
	 * 
	 * @param flapsMotor The motor controlling the flaps.
	 */
	public Aegis(EV3LargeRegulatedMotor flapsMotor){
		open = false;
		this.flapsMotor = flapsMotor;
		flapsMotor.setSpeed(400);
	}
	
	/**
	 * Opens the flaps to activate the defense system.
	 */
	public void open(){
		if(!open){
			flapsMotor.rotate((int)(360*4));
			open = true;
		}
	}
	
	/**
	 * Closes the flaps.
	 */
	public void close(){
		if(open){
			flapsMotor.rotate((int)(-360*4));
			open = false;
		}
	}
	
	/**
	 * Returns the state of the aegis system.
	 * 
	 * @return True if the flaps are openned, false if closed.
	 */
	public boolean isOpenned(){
		return open;
	}
}
