package finalProject;

import java.util.ArrayList;

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
	private static final int AVOID_DISTANCE = 40;
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
	 * Returns when the navigator reaches its initial target.
	 * and saves all moves made by the obstacle avoider in a double array.
	 * 
	 * @param direction The direction the robot will turn to when it sees a wall. True for left, false for right.
	 * @return A double[][] containing all moves made by the obstacle avoider in the form {x1, y1} , {x2, y2} ...
	 * The last {x, y} is always the final destination of the robot. As such, if the avoid() method did not have to
	 * avoid any obstacle, it will return {{x, y}} where x, y are the initial destination coordinates.
	 */
	public double[][] avoid(boolean direction){
		(new Thread(new Runnable(){
			public void run(){
				while(isPolling){
					usFilter.saturateSamples(PERIOD);
				}
			}
		})).start();
		double distance = usFilter.getFilteredData();
		ArrayList<double[]> checkPoints = new ArrayList<double[]>();
		int sign = direction ? 1 : -1;
		usFilter.saturateSamples(0);
		while(navigator.isNavigating()){
			if(distance < MIN_DISTANCE && distance > 0 && !navigator.isTurning()){
				navigator.pause();
				Sound.beep();
				double[] destination = {navigator.getTargetX(), navigator.getTargetY()};
				navigator.turnBy(Math.PI/2 * sign);
				navigator.waitForStop();
				navigator.travelTo(odometer.getX() + AVOID_DISTANCE * Math.cos(odometer.getTheta()), odometer.getY() + AVOID_DISTANCE * Math.sin(odometer.getTheta()));
				checkPoints.add(new double[]{navigator.getTargetX(), navigator.getTargetY()});
				navigator.waitForStop();
				navigator.turnBy(-Math.PI/2 * sign);
				navigator.waitForStop();
				navigator.travelTo(odometer.getX() + AVOID_DISTANCE * Math.cos(odometer.getTheta()), odometer.getY() + AVOID_DISTANCE * Math.sin(odometer.getTheta()));
				checkPoints.add(new double[]{navigator.getTargetX(), navigator.getTargetY()});
				navigator.waitForStop();
				navigator.setTarget(destination);
			}
			try{Thread.sleep(PERIOD);}catch(Exception e){}
			distance = usFilter.getFilteredData();
			navigator.move();
		}
		isPolling = false;
		return checkPoints.toArray(new double[checkPoints.size()][]);
	}
}
