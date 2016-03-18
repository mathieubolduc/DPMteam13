package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.Sound;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class ObstacleAvoider{
	
	//member variables
	Navigator navigator;
	Odometer odometer;
	Filter usFilter;
	private static final double MIN_DISTANCE = 0.3;
	private static final int AVOID_DISTANCE = 20;
	private static final int PERIOD = 50;
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
		this.usFilter = new Filter(Type.EMPTY, usSensor.getDistanceMode(), 1);
		this.isPolling = true;
	}
	
	/**
	 * Makes the robot avoid obstacles while trying to reach the navigator's current target.
	 * Waits until the navigator reaches its target to terminate.
	 */
	public void avoid(){
		(new Thread(new Runnable(){
			public void run(){
				while(isPolling){
					usFilter.saturateSamples(PERIOD);
				}
			}
		})).start();
		double distance = 0;
		while(navigator.isNavigating()){
			distance = usFilter.getFilteredData();
			if(distance < MIN_DISTANCE && distance > 0){
				Sound.beep();
				double[] destination = new double[]{navigator.getTargetX(), navigator.getTargetY()};
				navigator.turnBy(Math.PI);
				do{
					try{Thread.sleep(50);}catch(Exception e){}
					distance = usFilter.getFilteredData();
				}while(distance < MIN_DISTANCE && distance > 0);
				
				try{Thread.sleep(500);}catch(Exception e){}
				navigator.travelTo(odometer.getX() + AVOID_DISTANCE * Math.cos(odometer.getTheta()), odometer.getY() + AVOID_DISTANCE * Math.sin(odometer.getTheta()));
				navigator.waitForStop();
				navigator.turnBy(-Math.PI/2);
				navigator.waitForStop();
				navigator.travelTo(destination);
			}
			try{Thread.sleep(50);}catch(Exception e){}
		}
		isPolling = false;
	}
	
	public Filter getFilter(){
		return usFilter;
	}
}
