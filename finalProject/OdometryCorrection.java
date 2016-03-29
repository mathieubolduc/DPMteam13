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
	private static final double LIGHT_THRESHOLD = 0.06, SQUARE_LENGTH = 30.67, DIST_THRESHOLD = 7, ANGLE_THRESHOLD = Math.PI/6;
	private static final int PERIOD = 50;
	private final double SENSOR_DIST_TANGENT, SENSOR_DIST_NORMAL;
	private Odometer odometer;
	private Navigator navigator;
	private Filter leftFilter;
	private Filter rightFilter;
	private String lastCorrection;
	
	/**
	 * Constructor for OdometryCorrection.
	 * 
	 * @param odometer The odometer that needs to be corrected.
	 * @param Navigator The navigator of the robot.
	 * @param leftColorSensor The color sensor that will be used to see the black lines.
	 * If you are using only 1 color sensor, put it in this parameter.
	 * @param rightColorSensor The color sensor that will be used to see the black lines on the right.
	 * If you are using only 1 color sensor, leave this parameter null.
	 * @param sensorDistTangent The distance of the sensor to the center of rotation of the robot, tangent.
	 * @param sensorDistNormal The distance of the sensor to the center of rotation of the robot, normal.
	 * If you are using only 1 color sensor, this parameter is meaningless.
	 */
	public OdometryCorrection(Odometer odometer, Navigator navigator, EV3ColorSensor leftColorSensor, EV3ColorSensor rightColorSensor, double sensorDistTangent, double sensorDistNormal){
		this.odometer = odometer;
		this.navigator = navigator;
		this.leftFilter = new Filter(Type.DERIVATIVE, leftColorSensor.getRedMode(), 2);
		if(rightColorSensor != null){
			this.rightFilter = new Filter(Type.DERIVATIVE, rightColorSensor.getRedMode(), 2);
		}
		else{
			rightFilter = null;
		}
		this.SENSOR_DIST_TANGENT = sensorDistTangent;
		this.SENSOR_DIST_NORMAL = sensorDistNormal;
		this.lastCorrection = "";
	}
	
	//required for Thread
	public void run(){
		
		if(rightFilter != null){
				
			long start, end;
			double[][] sensorLocations = new double[2][2];
			Boolean[] lineTypes = new Boolean[2];	//true for horizontal, false for vertical, null for nothing
			int lastSensor = 0;	//the last sensor that saw a line, 0 for left, 1 for right
			double x=0, y=0, theta=0, offset=0, distance=0;
			leftFilter.saturateSamples(0);
			rightFilter.saturateSamples(0);
			
			while(true){
				start = System.currentTimeMillis();
				
				leftFilter.addSample();
				rightFilter.addSample();
				
				if(!navigator.isTurning()){
					//update the sensor positions if you see a black line
					//left sensor
					if(Math.abs(leftFilter.getFilteredData()) > LIGHT_THRESHOLD){
						lastSensor = 0;
						sensorLocations[0][0] = odometer.getX();
						sensorLocations[0][1] = odometer.getY();
						lineTypes[0] = Math.abs(Math.min(odometer.getX() % SQUARE_LENGTH, 30 - odometer.getX() % SQUARE_LENGTH))
							< Math.abs(Math.min(odometer.getY() % SQUARE_LENGTH, 30 - odometer.getY() % SQUARE_LENGTH));
					}
					
					//right sensor
					if(Math.abs(rightFilter.getFilteredData()) > LIGHT_THRESHOLD){
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
							
							offset = SENSOR_DIST_TANGENT*Math.cos(odometer.getTheta()) + SENSOR_DIST_NORMAL*Math.sin(odometer.getTheta()) * -(2*lastSensor-1);
							
							x = Math.round((odometer.getX() - offset)/SQUARE_LENGTH)*SQUARE_LENGTH + offset;
							if(Math.abs(x - odometer.getX()) < DIST_THRESHOLD)
								odometer.setX(x);
							
							distance = Math.sqrt(Math.pow(odometer.getX() - sensorLocations[(lastSensor+1)%2][0], 2) + Math.pow(odometer.getY() - sensorLocations[(lastSensor+1)%2][1], 2));
							if(distance < DIST_THRESHOLD){
								theta = Math.PI * (odometer.getTheta() / Math.PI) - (2*lastSensor-1)*Math.atan2(distance, 2*SENSOR_DIST_NORMAL);
								if(Utility.angleDiff(theta, odometer.getTheta()) < ANGLE_THRESHOLD)
									odometer.setTheta(theta);
							}
							
							lastCorrection = Utility.truncate("x: " + x, 7) + Utility.truncate(" T: " + theta, 9);
						}
						
						//if you are traveling in y and see a horizontal line
						if((odometer.getTheta() % Math.PI > Math.PI/4 && odometer.getTheta() % Math.PI < Math.PI*3/4) && lineTypes[0]){
							
							offset = SENSOR_DIST_TANGENT*Math.sin(odometer.getTheta()) + SENSOR_DIST_NORMAL*Math.cos(odometer.getTheta()) * (2*lastSensor-1);
							
							y = Math.round((odometer.getY() - offset)/SQUARE_LENGTH)*SQUARE_LENGTH + offset;
							if(Math.abs(y - odometer.getY()) < DIST_THRESHOLD)
								odometer.setY(y);
							
							distance = Math.sqrt(Math.pow(odometer.getX() - sensorLocations[(lastSensor+1)%2][0], 2) + Math.pow(odometer.getY() - sensorLocations[(lastSensor+1)%2][1], 2));
							if(distance < DIST_THRESHOLD){
								theta = (odometer.getTheta() < Math.PI ? Math.PI/2 : Math.PI*3/2) - (2*lastSensor-1)*Math.atan2(distance, 2*SENSOR_DIST_NORMAL);
								if(Utility.angleDiff(theta, odometer.getTheta()) < ANGLE_THRESHOLD)
									odometer.setTheta(theta);
							}
							
							lastCorrection = Utility.truncate("y: " + y, 7) + Utility.truncate(" T: " + theta, 9);
						}
						
						lineTypes[0] = null;
						lineTypes[1] = null;
					}
				}
				else{
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
		else{
			
			long start, end;
			double nearestX, nearestY, offset;
			leftFilter.saturateSamples(0);
			
			while(true){
				start = System.currentTimeMillis();

				leftFilter.addSample();
				
				if(Math.abs(leftFilter.getFilteredData()) > LIGHT_THRESHOLD){
					Sound.beep();
					offset = SENSOR_DIST_TANGENT*Math.cos(odometer.getTheta());
					nearestX = Math.round((odometer.getX() - offset)/SQUARE_LENGTH)*SQUARE_LENGTH + offset;
					offset = SENSOR_DIST_TANGENT*Math.sin(odometer.getTheta());
					nearestY = Math.round((odometer.getY() - offset)/SQUARE_LENGTH)*SQUARE_LENGTH + offset;
					if(Math.abs(odometer.getX()-nearestX) < Math.abs(odometer.getY()-nearestY)){
						odometer.setX(nearestX);
						lastCorrection = "x: " + nearestX;
					}
					else{
						odometer.setY(nearestY);
						lastCorrection = "y: " + nearestY;
					}
					try{Thread.sleep(1000);}catch(Exception e){}
					leftFilter.saturateSamples(0);
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
	
	public String toString(){
		return lastCorrection;
	}
}
