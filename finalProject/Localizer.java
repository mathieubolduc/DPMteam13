package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.Sound;
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
	private EV3UltrasonicSensor usSensor;
	private EV3ColorSensor colorSensor;
	private final double SENSOR_DISTANCE;
	private Navigator navigator;
	private Odometer odometer;
	private final static double LIGHT_THRESHOLD = 0.6, US_THRESHOLD = 0.4, CORRECTION = 0, NOISE_MARGIN = 0.04;
	private boolean recorded1 = false, recorded2 = true;
	private double theta1, theta2, thetaResult;
	private final static int COOLDOWN = 300;
	
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
	public Localizer(Navigator navigator, Odometer odometer, EV3UltrasonicSensor usSensor, EV3ColorSensor colorSensor, double sensorDistance){
		this.navigator = navigator;
		this.odometer = odometer;
		this.SENSOR_DISTANCE = sensorDistance;
		this.colorSensor = colorSensor;
		this.usSensor = usSensor;
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
	
	//performs a localization using the us sensor by turning 360 deg and detecting 2 walls.
	private void usLocalization(){
		
		//find the angle
		Filter usFilter = new Filter(Type.EMPTY, usSensor.getDistanceMode(), 1);
		double[] angles = new double[2];
		usFilter.saturateSamples(20);
		navigator.turnBy(Math.PI*2);
		
		//get the 1st angle
		//rotate until you see no wall
		while(usFilter.getFilteredData() <= US_THRESHOLD + NOISE_MARGIN){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}
		try{Thread.sleep(COOLDOWN);}catch(Exception e){}
		//rotate until you see a wall
		while(usFilter.getFilteredData() >= US_THRESHOLD + NOISE_MARGIN && !recorded1){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}

		//record angle when entering noise margin
		theta1 = odometer.getTheta();
		recorded1 = true;
		recorded2 = false;
		
		while(usFilter.getFilteredData() >= US_THRESHOLD - NOISE_MARGIN && !recorded2){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}
		
		//record angle when exiting noise margin
		theta2 = odometer.getTheta();
		recorded1 = false;
		recorded2 = true;
		navigator.pause();
		
		//usable angle is average of entry and exit angles
		angles[0] = (theta1+theta2)/2;
		
		theta1 = 0;
		theta2 = 0; //clear temporary storage
		
		Sound.beep();

		navigator.turnBy(-Math.PI*2);
		
		//get the 2nd angle
		
		//rotate until you see no wall
		
		while(usFilter.getFilteredData() <= US_THRESHOLD + NOISE_MARGIN){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}
		
		//rotate until you see a wall
		
		while(usFilter.getFilteredData() >= US_THRESHOLD + NOISE_MARGIN &&!recorded1){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}
		
		//record angle when entering noise margin
		theta1 = odometer.getTheta();
		recorded1 = true;
		recorded2 = false;
		
		while(usFilter.getFilteredData() >= US_THRESHOLD - NOISE_MARGIN && !recorded2){
			usFilter.addSample();
//			try{Thread.sleep(20);}catch(Exception e){}
		}
		
		//record angle when exiting noise margin
		
		theta2 = odometer.getTheta();
		recorded1 = false;
		recorded2 = true;
		navigator.pause();
		
		//usable angle is average of entry and exit angles
		
		angles[1] = (theta1+theta2)/2;
		
		theta1 = 0;
		theta2 = 0; //clear temporary storage
		
		Sound.beep();
		
		odometer.setTheta((angles[0] < angles[1] ? 225d/180*Math.PI : 45d/180*Math.PI) -(angles[0] + angles[1]) / 2 + odometer.getTheta() + CORRECTION);
		
		//find the Y
		navigator.turnTo(Math.PI*3/2);
		navigator.waitForStop();
		Sound.beep();
		usFilter.saturateSamples(20);
		odometer.setY(100*usFilter.getFilteredData()-30);
		
		//find the X
		navigator.turnTo(Math.PI);
		navigator.waitForStop();
		Sound.beep();
		usFilter.saturateSamples(20);
		odometer.setX(100*usFilter.getFilteredData()-30);
		
	}
	
	//performs a localization using the light sensor by turning 360 deg and detecting 4 lines.
	private void lightLocalization(){
		Filter lightFilter = new Filter(Type.DERIVATIVE, colorSensor.getRedMode(), 5);
		double[] angles = new double[4];
		int i=0;
		long time;
		//turn 360 deg to hopefully go over 4 lines
		navigator.turnBy(Math.PI*2);
		while(navigator.isNavigating()){
			lightFilter.addSample();
			//if there is a line
			if(Math.abs(lightFilter.getFilteredData()) > LIGHT_THRESHOLD){
//				navigator.pause();
				angles[i] = odometer.getTheta();
				i++;
				Sound.beep();
				time = System.currentTimeMillis();
				while(System.currentTimeMillis() - time < COOLDOWN){
					lightFilter.addSample();
					try{Thread.sleep(20);}catch(Exception e){}
				}
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
