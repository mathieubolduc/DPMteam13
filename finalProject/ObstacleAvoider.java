package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.Sound;
import lejos.hardware.sensor.EV3UltrasonicSensor;

/**
 * The class responsible for avoiding objects.
 * 
 * @author Mathieu Bolduc
 * @version 1.0
 *
 */
public class ObstacleAvoider{
	
	//member variables
	private Navigator navigator;
	private Odometer odometer;
	public Filter usFilter;
	private static final double MIN_DISTANCE = 0.3;
	private static final int AVOID_DISTANCE = 30;
	private static final int PERIOD = 10;
	private boolean isPolling;
	
	
	/**
	 * Constructor for ObstacleAvoider.
	 * 
	 * @param navigator The robot's navigator.
	 * @param odometer The robot's odometer.
	 * @param usSensor The US sensor that will detect the walls in front of the robot.
	 */
	public ObstacleAvoider(Navigator navigator, Odometer odometer, EV3UltrasonicSensor usSensor){
		this.navigator = navigator;
		this.odometer = odometer;
		this.usFilter = new Filter(Type.AVERAGE, usSensor.getDistanceMode(), 5);
		this.isPolling = true;
	}
	
	/**
	 * Makes the robot avoid obstacles while trying to reach the navigator's current target.
	 * Waits until the navigator reaches its target to terminate.
	 * 
	 * @param direction The direction the robot will turn to when it sees a wall. True for left, false for right.
	 */
	public void avoid(boolean direction){
		(new Thread(new Runnable(){
			public void run(){
				while(isPolling){
					usFilter.saturateSamples(PERIOD);
				}
			}
		})).start();
		double distance = usFilter.getFilteredData();
		int sign = direction ? 1 : -1;
		usFilter.saturateSamples(0);
		while(navigator.isNavigating()){
			if(distance < MIN_DISTANCE && distance > 0 && !navigator.isTurning()){
				navigator.pause();
				Sound.beep();
				double[] destination = new double[]{navigator.getTargetX(), navigator.getTargetY()};
				navigator.turnBy(Math.PI/2 * sign);
				navigator.waitForStop();
				navigator.travelTo(odometer.getX() + AVOID_DISTANCE * Math.cos(odometer.getTheta()), odometer.getY() + AVOID_DISTANCE * Math.sin(odometer.getTheta()));
				navigator.waitForStop();
				navigator.turnBy(Math.PI * 0.6 * -sign);
				navigator.waitForStop();
				navigator.setTarget(destination);
			}
			try{Thread.sleep(PERIOD);}catch(Exception e){}
			distance = usFilter.getFilteredData();
			navigator.move();
		}
		isPolling = false;
	}
}
