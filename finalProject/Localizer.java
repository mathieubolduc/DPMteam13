package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

/**
 * The class responsible for determining the robot's initial position.
 * 
 * @author Mathieu
 * @version 1.0
 */
public class Localizer {
	
	//member variables
	private Filter lightFilter, usFilter;
	private final double SENSOR_DISTANCE;
	private Navigator navigator;
	private Odometer odometer;
	private EV3LargeRegulatedMotor usMotor;
	private final static double LIGHT_THRESHOLD = 1, US_THRESHOLD = 20;
	private final static int COOLDOWN = 100;
	
	/**
	 * Constructor for Localiser.
	 * 
	 * @param navigator The robot's navigator.
	 * @param odometer The robot's odometer whose values are to be set.
	 * @param usMotor The motor responsible for rotating the ultrasonic sensor.
	 * @param usSensor The ultrasonic sensor.
	 * @param colorSensor The color sensor.
	 * @param colorDistance The distance between the color sensor and the center of rotation of the robot.
	 */
	public Localizer(Navigator navigator, Odometer odometer, EV3LargeRegulatedMotor usMotor, EV3UltrasonicSensor usSensor, EV3ColorSensor colorSensor, double sensorDistance){
		this.navigator = navigator;
		this.odometer = odometer;
		this.usMotor = usMotor;
		this.SENSOR_DISTANCE = sensorDistance;
		this.lightFilter = new Filter(Type.DERIVATIVE, colorSensor.getRedMode(), 5);
		this.usFilter = new Filter(Type.MEDIAN, usSensor.getDistanceMode(), 5);
	}
	
	/**
	 * Determines the current position of the robot and updates the odometer accordingly.
	 * 
	 * @param t The type of localization to perform.
	 */
	public void localize(type t){
		switch(t){
			case LIGHT:
				this.lightLocalization();
				break;
			case US:
				this.usLocalization();
				break;
			case US_AND_LIGHT:
				this.usLocalization();
				Sound.beep();
				navigator.travelTo(25, 25);
				navigator.turnTo(Math.PI/4);
				this.lightLocalization();
				break;
		}
		Sound.beep();
	}
	
	public enum type{
		/**
		 * Performs the light localization.
		 */
		LIGHT,
		
		/**
		 * Performs the US localization.
		 */
		US,
		
		/**
		 * Performs the US localization first, followed by the light localization.
		 */
		US_AND_LIGHT
	}
	
	private void usLocalization(){
		
	}
	
	private void lightLocalization(){
		double[] angles = new double[4];
		int i=0;
		//turn 360 deg to hopefully go over 4 lines
		navigator.turnBy(Math.PI*2);
		while(navigator.isNavigating()){
			lightFilter.addSample();
			//if there is a line, record the angle
			if(lightFilter.getFilteredData() > LIGHT_THRESHOLD){
				angles[i] = odometer.getTheta();
				Sound.beep();
				try{Thread.sleep(COOLDOWN);}catch(Exception e){}
			}
			else{
				try{Thread.sleep(50);}catch(Exception e){}
			}
		}
		
		//set the correct coordinates and angle
		odometer.setX(-SENSOR_DISTANCE * Math.cos((angles[0] - angles[2]) / 2));
		odometer.setY(-SENSOR_DISTANCE * Math.cos((angles[1] - angles[3]) / 2));
		odometer.setTheta(odometer.getTheta() + Math.PI - (angles[0] + angles[2])/2);
	}
}
