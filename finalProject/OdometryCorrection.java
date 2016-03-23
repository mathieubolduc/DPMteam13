package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.Sound;
import lejos.hardware.sensor.EV3ColorSensor;

/**
 * The class responsible for updating the odometer when the robot encounters black lines.
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 *
 */
public class OdometryCorrection extends Thread{
	
	//member variables
	private static final double THRESHOLD = 0.06, SQUARE_LENGTH = 30.67;
	private static final int PERIOD = 50;
	private final double SENSOR_DIST_TANGENT, SENSOR_DIST_NORMAL;
	private Odometer odometer;
	private Filter leftFilter;
	private Filter rightFilter;
	
	/**
	 * Constructor for OdometryCorrection.
	 * 
	 * @param odometer The odometer that needs to be corrected.
	 * @param colorSensor The color sensor that will be used to see the black lines.
	 */
	public OdometryCorrection(Odometer odometer, EV3ColorSensor leftColorSensor, EV3ColorSensor rightColorSensor, double sensorDistTangent, double sensorDistNormal){
		this.odometer = odometer;
		this.leftFilter = new Filter(Type.DERIVATIVE, leftColorSensor.getRedMode(), 2);
		this.rightFilter = new Filter(Type.DERIVATIVE, rightColorSensor.getRedMode(), 2);
		this.SENSOR_DIST_TANGENT = sensorDistTangent;
		this.SENSOR_DIST_NORMAL = sensorDistNormal;
	}
	
	//required for Thread
	public void run(){
		long start, end;
		double[][] sensorLocations = new double[2][2];
		Boolean[] lineTypes = new Boolean[2];	//true for horizontal, false for vertical, null for nothing
		int lastSensor = 0;	//the last sensor that saw a line, 0 for left, 1 for right
		
		while(true){
			start = System.currentTimeMillis();
			
			leftFilter.addSample();
			rightFilter.addSample();
			
			//update the sensor positions if you see a black line
			//left sensor
			if(Math.abs(leftFilter.getFilteredData()) < THRESHOLD){
				Sound.beep();
				lastSensor = 0;
				sensorLocations[0][0] = odometer.getX();
				sensorLocations[0][1] = odometer.getY();
				lineTypes[0] = Math.abs(Math.min(odometer.getX() % SQUARE_LENGTH, 30 - odometer.getX() % SQUARE_LENGTH))
					< Math.abs(Math.min(odometer.getY() % SQUARE_LENGTH, 30 - odometer.getY() % SQUARE_LENGTH));
			}
			
			//right sensor
			if(Math.abs(rightFilter.getFilteredData()) < THRESHOLD){
				Sound.beep();
				lastSensor = 1;
				sensorLocations[1][0] = odometer.getX();
				sensorLocations[1][1] = odometer.getY();
				lineTypes[1] = Math.abs(Math.min(odometer.getX() % SQUARE_LENGTH, 30 - odometer.getX() % SQUARE_LENGTH))
						< Math.abs(Math.min(odometer.getY() % SQUARE_LENGTH, 30 - odometer.getY() % SQUARE_LENGTH));
			}
			
			//make a correction if left and right see the same type of line and they are not null
			if(lineTypes[0] != null && lineTypes[0] == lineTypes[1]){
				
				//if you are traveling in x and see a vertical line
				if((odometer.getTheta() % Math.PI < Math.PI/4 || odometer.getTheta() % Math.PI > Math.PI*3/4) && !lineTypes[0]){
					
					odometer.setX(Math.round(odometer.getX()/SQUARE_LENGTH)*SQUARE_LENGTH + sensorLocations[lastSensor][0] 
							+ SENSOR_DIST_TANGENT*Math.cos(odometer.getTheta()) 
							+ SENSOR_DIST_NORMAL*Math.sin(odometer.getTheta()) * -(2*lastSensor-1));
					
					odometer.setTheta(Math.PI * (odometer.getTheta() / Math.PI) - (2*lastSensor-1)*Math.atan2(2*SENSOR_DIST_NORMAL, Math.sqrt(Math.pow(odometer.getX() - sensorLocations[(lastSensor+1)%2][0], 2) + Math.pow(odometer.getY() - sensorLocations[(lastSensor+1)%2][1], 2))));
				}
				
				//if you are traveling in y and see a horizontal line
				if((odometer.getTheta() % Math.PI > Math.PI/4 && odometer.getTheta() % Math.PI < Math.PI*3/4) && lineTypes[0]){
					
					odometer.setY(Math.round(odometer.getY()/SQUARE_LENGTH)*SQUARE_LENGTH + sensorLocations[lastSensor][1] 
						+ SENSOR_DIST_TANGENT*Math.sin(odometer.getTheta()) 
						+ SENSOR_DIST_NORMAL*Math.cos(odometer.getTheta()) * (2*lastSensor-1));
					
					odometer.setTheta((odometer.getTheta() < Math.PI ? Math.PI/2 : Math.PI*3/2) - (2*lastSensor-1)*Math.atan2(2*SENSOR_DIST_NORMAL, Math.sqrt(Math.pow(odometer.getX() - sensorLocations[(lastSensor+1)%2][0], 2) + Math.pow(odometer.getY() - sensorLocations[(lastSensor+1)%2][1], 2))));
				}
				
				lineTypes[0] = null;
				lineTypes[1] = null;
			}
			

			// this ensures that the odometry correction only runs once every period
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
}
