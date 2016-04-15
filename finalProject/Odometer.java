package finalProject;


import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3GyroSensor;

/**
 * The class responsible for tracking the robot's position and orientation.
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 */
public class Odometer extends Thread {
	
	//member variables
	private double x, y, theta;
	private final double TRACK, WHEEL_RADIUS;
	private final static double CORRECTION = 1.016;
	private final static int PERIOD = 15;	//the maximum speed the odometer can operate at
	private final EV3LargeRegulatedMotor leftMotor;
	private final EV3LargeRegulatedMotor rightMotor;
	private final EV3GyroSensor gyroSensor;
	private Object lock;

	
	/**
	 * Constructor for the odometer.
	 * 
	 * @param leftMotor The left motor.
	 * @param rightMotor The right motor.
	 * @param gyroSensor The gyro sensor if there is one. If left as null, the angle will be calculated from the motors.
	 * @param track The distance between the two wheels.
	 * @param wheelRadius The radius of the wheels.
	 */
	public Odometer(EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor, EV3GyroSensor gyroSensor, double track, double wheelRadius) {
		x = 0.0;
		y = 0.0;
		theta = 0.0;
		lock = new Object();
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		this.gyroSensor = gyroSensor;
		this.TRACK = track;
		this.WHEEL_RADIUS = wheelRadius;
		if(gyroSensor != null)
			gyroSensor.reset();
	}

	
	// run method (required for Thread)
	public void run() {
		//initialize variables
		long updateStart, updateEnd;
		int tachoL = 0;
		int tachoR = 0;
		int currentTachoL = 0;
		int currentTachoR = 0;
		int lastTachoL = 0;
		int lastTachoR = 0;
		double distance = 0;
		double deltaTheta = 0;
		float[] sample = new float[1];

		while (true) {
			updateStart = System.currentTimeMillis();
			
			//get tacho changes for both motors
			currentTachoL = leftMotor.getTachoCount();
			currentTachoR = rightMotor.getTachoCount();
			tachoL = currentTachoL - lastTachoL;
			tachoR = currentTachoR - lastTachoR;
			lastTachoL = currentTachoL;
			lastTachoR = currentTachoR;
			
			
			synchronized (lock) {
				
				//calculate the change in distance
				distance = (tachoR*CORRECTION + tachoL)/360d * Math.PI * WHEEL_RADIUS;
				
				//calculate the change in theta. Use the gyro sensor if there is one, else use the tachos
				if(gyroSensor == null){
					deltaTheta = (tachoR*CORRECTION - tachoL)/(360d * TRACK) * 2 * Math.PI * WHEEL_RADIUS;
				}
				else{
					gyroSensor.getAngleMode().fetchSample(sample, 0);
					//convert the angle to our notation: between 0 and 2pi, counterclockwise
					sample[0] %= 360;
					if(sample[0] < 0){
						sample[0] = 360 + sample[0];
					}
					sample[0] = (float) (Math.PI*(sample[0])/180);
					deltaTheta = Math.signum(tachoR - tachoL) * Utility.angleDiff(sample[0], theta);
					//if the gyro sensor is messed up and gives a reading too far off, read the tachos instead
					if(Math.abs(deltaTheta) > Math.PI/2){
						deltaTheta = (tachoR - tachoL)/(360d * TRACK) * 2 * Math.PI * WHEEL_RADIUS;
					}
					
				}
				
				//update x, y and theta
				x += distance * Math.cos(theta + deltaTheta/2);
				y += distance * Math.sin(theta + deltaTheta/2);
				theta += deltaTheta;
				
				//theta must be between 0 and 2pi rads
				theta %= Math.PI*2;
				if(theta < 0)
					theta += 2*Math.PI;
			}
			
			// this ensures that the odometer only runs once every period
			updateEnd = System.currentTimeMillis();
			if (updateEnd - updateStart < PERIOD) {
				try {
					Thread.sleep(PERIOD - (updateEnd - updateStart));
				} catch (InterruptedException e) {
					// nothing to do here
				}
			}
		}
	}
	
	
	//getters and setters
	
	/**
	 * Sets the x value of the odometer.
	 * 
	 * @param x The new x value.
	 */
	public void setX(double x) {
		synchronized (lock) {
			this.x = x;
		}
	}

	/**
	 * Sets the y value of the odometer.
	 * 
	 * @param y The new y value.
	 */
	public void setY(double y) {
		synchronized (lock) {
			this.y = y;
		}
	}

	/**
	 * Sets the theta value of the odometer. If there is a gyro, the robot MUST be stationary, and theta will be set to 0.
	 * 
	 * @param theta The new theta value. Must be in rads between 0 and 2pi.
	 */
	public void setTheta(double theta) {
		synchronized (lock) {
			//if there is a gyro sensor, reset it
			if(gyroSensor != null){
				gyroSensor.reset();
				theta = 0;
			}
			this.theta = theta;
		}
	}
	
	/**
	 * Returns the current x value of the odometer.
	 * 
	 * @return The current x value of the odometer.
	 */
	public double getX() {
		double result;
		synchronized (lock) {
			result = x;
		}
		return result;
	}

	/**
	 * Returns the current y value of the odometer.
	 * 
	 * @return The current y value of the odometer.
	 */
	public double getY() {
		double result;
		synchronized (lock) {
			result = y;
		}
		return result;
	}

	/**
	 * Returns the current theta value of the odometer.
	 * 
	 * @return The current angle of the odometer. In rads between 0 and 2pi, starting counterclockwise from the positive x axis.
	 */
	public double getTheta() {
		double result;
		synchronized (lock) {
			result = theta;
		}
		return result;
	}
	
	/**
	 * Returns a string representation of the odometer's position which fits the EV3's LCD screen.
	 * 
	 * @return The odometer's x, y and theta, in string form.
	 */
	public String toString(){
		return Utility.truncate("x: " + x, 16) + Utility.truncate("y: " + y, 16) + Utility.truncate("t: " + theta, 16);
		
	}
}