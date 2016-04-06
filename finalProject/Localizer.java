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
	private int corner;
	private Navigator navigator;
	private Odometer odometer;
	private final static double LIGHT_THRESHOLD = 0.2, US_THRESHOLD = 0.4, US_CORRECTION = 0.1, LIGHT_CORRECTION = 0.05, NOISE_MARGIN = 0.04, SQUARE_LENGTH = 30.67;
	private boolean recorded1 = false, recorded2 = true;
	private double theta1, theta2;
	private final static int COOLDOWN = 300;
	
	/**
	 * Constructor for Localizer.
	 * 
	 * @param navigator The robot's navigator.
	 * @param odometer The robot's odometer whose values are to be set.
	 * @param usSensor The ultrasonic sensor.
	 * @param colorSensor The color sensor.
	 * @param sensorDistance The distance between the color sensor and the center of rotation of the robot.
	 * @param corner The corner the robots starts at. 1 for lower left, 2 for lower right, 3 for upper right, 4 for upper left.
	 */
	public Localizer(Navigator navigator, Odometer odometer, EV3UltrasonicSensor usSensor, EV3ColorSensor colorSensor, double sensorDistance, int corner){
		this.navigator = navigator;
		this.odometer = odometer;
		this.SENSOR_DISTANCE = sensorDistance;
		this.colorSensor = colorSensor;
		this.usSensor = usSensor;
		this.corner = corner-1;
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
	}
	
	public enum type{
		/**
		 * Performs the light localization.
		 * The robot must be close enough to a line intersection to see 4 lines.
		 * If the robot does not see 4 lines, it will not correct anything.
		 */
		LIGHT,
		
		/**
		 * Performs the US localization. The robot must be in a corner.
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
		Filter usFilter = new Filter(Type.AVERAGE, usSensor.getDistanceMode(), 5);
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
		try{Thread.sleep(COOLDOWN);}catch(Exception e){}
		usFilter.saturateSamples(10);
		
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
		
		odometer.setTheta((angles[0] < angles[1] ? 225d/180*Math.PI : 45d/180*Math.PI) -(angles[0] + angles[1]) / 2 + odometer.getTheta() + US_CORRECTION + corner*Math.PI/2);
		
		//find the Y
		navigator.turnTo(corner < 2 ? Math.PI*3/2 : Math.PI/2);
		navigator.waitForStop();
		Sound.beep();
		usFilter.saturateSamples(20);
		if(corner < 2)
			odometer.setY(100*usFilter.getFilteredData() - 25);
		else
			odometer.setY(25 - 100*usFilter.getFilteredData() + SQUARE_LENGTH*10);
			
		//find the X
		navigator.turnTo((corner == 0 || corner == 3) ? Math.PI : 0);
		navigator.waitForStop();
		Sound.beep();
		usFilter.saturateSamples(20);
		if(corner == 0 || corner == 3)
			odometer.setX(100*usFilter.getFilteredData()-25);
		else
			odometer.setX(25 - 100*usFilter.getFilteredData() + SQUARE_LENGTH*10);
		
	}
	
	//performs a localization using the light sensor by turning 360 deg and detecting 4 lines.
	private void lightLocalization(){
		Filter lightFilter = new Filter(Type.DERIVATIVE, colorSensor.getRedMode(), 2);
		double[] angles = new double[4];
		int i=0;
		long time;
		int quadrant = (int) (odometer.getTheta() / (Math.PI/2));
		
		//turn the the closest 45deg to make sure you dont start near a line
		navigator.turnTo(quadrant*(Math.PI/2) + Math.PI/4);
		navigator.waitForStop();
		lightFilter.saturateSamples(50);
		
		//turn 360 deg to hopefully go over 4 lines
		navigator.turnBy(Math.PI*2);
		while(navigator.isNavigating()){
			lightFilter.addSample();
			//if there is a line
			if(Math.abs(lightFilter.getFilteredData()) > LIGHT_THRESHOLD){
				try{angles[i] = odometer.getTheta();}catch(ArrayIndexOutOfBoundsException e){}
				i++;
				Sound.beep();
				time = System.currentTimeMillis();
				while(System.currentTimeMillis() - time < COOLDOWN){
					lightFilter.addSample();
					try{Thread.sleep(20);}catch(Exception e){}
				}
			}
			else{
				try{Thread.sleep(20);}catch(Exception e){}
			}
		}
		
		//if you didnt see all 4 lines
		if(i != 4){
			Sound.buzz();
			return;
		}
		
		//set the correct coordinates and angle
		//the corrections are for quadrant 0. For other quadrants just shift the angle index by the quadrant (mod 4)
		double corrX = -SENSOR_DISTANCE * Math.cos((angles[(0+quadrant)%4] - angles[(2+quadrant)%4]) / 2);
		double corrY = (quadrant < 2 ? -1 : 1) * SENSOR_DISTANCE * Math.cos((angles[(1+quadrant)%4] - angles[(3+quadrant)%4]) / 2);
		double corrT = Math.PI - (angles[(0+quadrant)%4] + angles[(2+quadrant)%4])/2;
		
		odometer.setX(corrX + Math.round(odometer.getX() / SQUARE_LENGTH) * SQUARE_LENGTH);
		odometer.setY(corrY + Math.round(odometer.getY() / SQUARE_LENGTH) * SQUARE_LENGTH);
		odometer.setTheta(corrT + odometer.getTheta() + LIGHT_CORRECTION);
	}
}