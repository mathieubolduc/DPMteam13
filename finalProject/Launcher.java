package finalProject;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The class responsible for launching the ball
 * 
 * @author Dennis Liu
 * @version 1.0
 */
public class Launcher{
	
	//member variables
	private Odometer odometer;
	private EV3LargeRegulatedMotor launchMotor;
	private double launchAcceleration = 2000; //the speed that the launchMotor operates at
	private Object lock;
	
	
	/**
	 * Constructor for Launcher
	 * 
	 * @param launchMotor The robot's motor responsible for launching mechanism.
	 */
	public Launcher(EV3LargeRegulatedMotor launchMotor){
		this.launchMotor = launchMotor;
		lock = new Object();
	}
	
	public enum mode{
		/**
		 * performs the retrieval of the ball
		 */
		GRAB,
		
		/**
		 * performs the launch of the ball
		 */
		SHOOT,
		
		/**
		 * resets the launchMotor position
		 */
		RESET
	}
	
	/**
	 * Rotates the launchMotor to perform launching and grabbing
	 * 
	 * @param m The mode that the launcher is operating in
	 */
	public void launch(mode m){
		synchronized (lock){
			switch(m){
			case GRAB:
				launchMotor.rotate(-420);
				break;
			case SHOOT:
				launchMotor.rotate(-120); 
				break;
			case RESET:
				launchMotor.rotateTo(-40);
				launchMotor.flt();
				try{Thread.sleep(500);}catch(Exception e){}
				launchMotor.stop();
			}
		}
	}
}
