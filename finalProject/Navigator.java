package finalProject;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * The class responsible for moving the robot.
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 */
public class Navigator extends Thread{
	
	//member variables
	private Odometer odometer;
	private EV3LargeRegulatedMotor leftMotor, rightMotor;
	private double targetX, targetY, targetT, relativeT;
	private static final double TOLERANCE = 1.0, LOOSE_ANGLE_TOLERANCE = Math.PI / 60, HARD_ANGLE_TOLERANCE = Math.PI / 180;
	private int FORWARD_SPEED = 400, ROTATE_SPEED = 200, CORRECT_SPEED = 10;
	private static final double MIN_SPEED_RATIO = 0.2;
	private static final int PERIOD = 50;
	private Object lock;
	private boolean navigating;
	private boolean forward;
	private boolean turning;
	
	
	/**
	 * Constructor for Navigator.
	 * 
	 * @param odometer The odometer that the navigator will get its position from. Must have getters for x, y and theta.
	 * @param leftMotor The left motor.
	 * @param rightMotor The right motor.
	 */
	public Navigator(Odometer odometer, EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor){
		this.odometer = odometer;
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
		navigating = false;
		turning = false;
		forward = true;
		targetX = Double.NaN; //targets are set to NaN whenever we do not want to have any target
		targetY = Double.NaN;
		targetT = Double.NaN;
		relativeT = Double.NaN;
		lock = new Object();
	}
	
	
	
	//run method (required for Thread)
	public void run(){
		
		long navigateStart, navigateEnd;
		double lastTheta = 0, distance = 0, angleTolerance;
		int leftCorrection = 0, rightCorrection = 0;
		int rotateSpeed, forwardSpeed;
		
		while(true){
			navigateStart = System.currentTimeMillis();
			
			synchronized (lock) {
					
				//recalculate targetT
				calculateTargetT();
				
				//the navigator should be used only if there is a destination and we are navigating towards it
				if(navigating){
					leftMotor.setAcceleration(1500);
					rightMotor.setAcceleration(1500);
					
					if(!Double.isNaN(targetT) || !Double.isNaN(targetX)){
						//the navigator prioritizes correcting absolute angles, then position, then relative angles.
						
						if(!Double.isNaN(targetX)){
							distance = Math.sqrt(Math.pow(odometer.getX() - targetX, 2) + Math.pow(odometer.getY() - targetY, 2));
							angleTolerance = LOOSE_ANGLE_TOLERANCE;
						}
						else{
							angleTolerance = HARD_ANGLE_TOLERANCE;
						}
						
						
						//check if theta is off-course
						if(!Double.isNaN(targetT) && Utility.angleDiff(odometer.getTheta(), targetT) > angleTolerance){
							navigating = true;
							turning = true;
							//correct the angle by turning
							rotateSpeed = (int) (Math.min(Utility.angleDiff(odometer.getTheta(), targetT) + MIN_SPEED_RATIO, 1) * ROTATE_SPEED);
							leftMotor.setSpeed(rotateSpeed);
							rightMotor.setSpeed(rotateSpeed);
							//calculate the most efficient direction to turn to
							if(odometer.getTheta() - targetT > 0 && odometer.getTheta() - targetT < Math.PI || odometer.getTheta() - targetT < -Math.PI){
								//turn right
								leftMotor.forward();
								rightMotor.backward();
							}
							else{
								//turn left
								leftMotor.backward();;
								rightMotor.forward();;
							}
						}
						//check if the position is off-course
						else if(!Double.isNaN(targetX) &&  distance > TOLERANCE){
							navigating = true;
							turning = false;
							forwardSpeed = (int) (Math.min(distance/10 + MIN_SPEED_RATIO, 1) * FORWARD_SPEED);
							if(Utility.angleDiff(odometer.getTheta(), targetT) > HARD_ANGLE_TOLERANCE){
								if(odometer.getTheta() > targetT || (targetT - odometer.getTheta()) > Math.PI*2-LOOSE_ANGLE_TOLERANCE){
									//correct towards left
									leftCorrection = 0;
									rightCorrection = CORRECT_SPEED;
								}
								else{
									//correct towards right
									leftCorrection = CORRECT_SPEED;
									rightCorrection = 0;
								}
							}
							if(forward){
								leftMotor.setSpeed(forwardSpeed - leftCorrection);
								rightMotor.setSpeed(forwardSpeed - rightCorrection);
								leftMotor.forward();
								rightMotor.forward();
							}
							else{
								leftMotor.setSpeed(forwardSpeed - rightCorrection);
								rightMotor.setSpeed(forwardSpeed - leftCorrection);
								leftMotor.backward();
								rightMotor.backward();
							}
						}
						//nothing to correct: the robot is at its destination
						else{
							navigating = false;
							turning = false;
							forward = true;
							leftMotor.stop(true);
							rightMotor.stop();
							targetX = Double.NaN;
							targetY = Double.NaN;
							targetT = Double.NaN;
						}
					}
					
					//theta, x and y are all NaN, so check if relative turning should be used
					else if(!Double.isNaN(relativeT)){
						//if the relative angle is small enough, stop turning
						if(Math.abs(relativeT) < LOOSE_ANGLE_TOLERANCE){
							relativeT = Double.NaN;
							navigating = false;
							turning = false;
							leftMotor.stop(true);
							rightMotor.stop();
						}
						//else turn and update the relative angle
						else{
							navigating = true;
							turning = true;
							rotateSpeed = (int) (Math.min(Math.abs(relativeT) + MIN_SPEED_RATIO, 1) * ROTATE_SPEED);
							leftMotor.setSpeed(rotateSpeed);
							rightMotor.setSpeed(rotateSpeed);
							if(relativeT > 0){
								//turn left
								leftMotor.backward();
								rightMotor.forward();
								relativeT -= Utility.angleDiff(odometer.getTheta(), lastTheta);
							}
							else{
								//turn right
								leftMotor.forward();
								rightMotor.backward();
								relativeT += Utility.angleDiff(odometer.getTheta(), lastTheta);
							}
						}
					}
				}
			}
			
			lastTheta = odometer.getTheta();
			
			// this ensures that the navigator only runs once every period
			navigateEnd = System.currentTimeMillis();
			if (navigateEnd - navigateStart < PERIOD) {
				try {
					Thread.sleep(PERIOD - (navigateEnd - navigateStart));
				} catch (InterruptedException e) {
					// nothing to do here
				}
			}
		
		}
	}
	
	//calculates the angle theta (from the positive X axis) between the robot's current position and its target position
	private void calculateTargetT(){
		//only calculate if the position and angle are not NaN and if the robot isnt in the right position
		if(!Double.isNaN(targetT) && !Double.isNaN(targetX) && !Double.isNaN(targetY)  &&  Math.abs(odometer.getX() - targetX) > TOLERANCE || Math.abs(odometer.getY() - targetY) > TOLERANCE){
			targetT = Math.atan2(targetY - odometer.getY(), targetX - odometer.getX());
			if(targetT < 0)
				targetT += Math.PI * 2;
			if(!forward)
				targetT = (targetT + Math.PI) % (Math.PI * 2);
		}
	}
	
	
	
	/**
	 * Turns the robot to the specified absolute angle theta using the shortest possible way (within ~3 degrees).
	 * Does not change the position of the robot: it turns on itself.
	 * Cancels any previous move commands.
	 * 
	 * @param theta The absolute angle to turn to. In rads between 0 and 2pi, counterclockwise from the positive x axis.
	 */
	public void turnTo(double theta){
		synchronized (lock) {
			navigating = true;
			forward = true;
			turning = true;
			targetT = theta;
			targetX = Double.NaN;
			targetY = Double.NaN;
			relativeT = Double.NaN;
		}
	}
	
	/**
	 * Turns by the specified angle relative to the robot's current angle (within ~3 degrees).
	 * Does not change the position of the robot: it turns on itself.
	 * Cancels any previous move commands.
	 * 
	 * @param theta The angle to turn by. In rads between 0 and 2pi, positive for left, negative for right.
	 */
	public void turnBy(double theta){
		synchronized (lock) {
			navigating = true;
			turning = true;
			forward = true;
			targetX = Double.NaN;
			targetY = Double.NaN;
			targetT = Double.NaN;
			relativeT = theta;
		}
	}
	
	/**
	 * Turns by towards the specified coordinates (within ~3 degrees).
	 * Does not change the position of the robot: it turns on itself.
	 * Cancels any previous move commands.
	 * 
	 * @param x The x coordinate of the point the robot should face.
	 * @param y The y coordinate of the point the robot should face.
	 */
	public void turnToward(double x, double y){
		synchronized (lock) {
			targetX = x;
			targetY = y;
			targetT = 0.0;
			relativeT = Double.NaN;
			calculateTargetT();
		}
		turnTo(targetT);
	}
	
	/**
	 * Moves the robot to the specified coordinates in a straight line (within ~1cm).
	 * Minor corrections may be done along the path so that the robot always faces its destination.
	 * The final orientation of the robot is somewhat random due to those corrections.
	 * Cancels any previous move commands.
	 * 
	 * @param x The x coordinate of the target destination.
	 * @param y The y coordinate of the target destination.
	 */
	public void travelTo(double x, double y){
		synchronized (lock) {
			navigating = true;
			turning = false;
			forward = true;
			targetX = x;
			targetY = y;
			targetT = 0.0;
			relativeT = Double.NaN;
			calculateTargetT();
		}
	}

	/**
	 * Moves the robot to the specified coordinates in a straight line (within ~1cm).
	 * Minor corrections may be done along the path so that the robot always faces its destination.
	 * The final orientation of the robot is somewhat random due to those corrections.
	 * Cancels any previous move commands.
	 * 
	 * @param x The x coordinate of the target destination.
	 * @param y The y coordinate of the target destination.
	 * @param forward True for forward, false for backward.
	 */
	public void travelTo(double x, double y, boolean forward){
		synchronized (lock) {
			navigating = true;
			turning = false;
			this.forward = forward;
			targetX = x;
			targetY = y;
			targetT = 0.0;
			relativeT = Double.NaN;
			calculateTargetT();
		}
	}

	/**
	 * Moves the robot to the specified coordinates in a straight line (within ~1cm).
	 * Minor corrections may be done along the path so that the robot always faces its destination.
	 * The final orientation of the robot is somewhat random due to those corrections.
	 * Cancels any previous move commands.
	 * 
	 * @param destination The array representing the target destination in the form {x, y}.
	 */
	public void travelTo(double[] destination){
		travelTo(destination[0], destination[1]);
	}
	
	/**
	 * Moves the robot to the specified coordinates in a straight line (within ~1cm).
	 * Minor corrections may be done along the path so that the robot always faces its destination.
	 * The final orientation of the robot is somewhat random due to those corrections.
	 * Cancels any previous move commands.
	 * 
	 * @param destination The array representing the target destination in the form {x, y}.
	 * @param forward True for forward, false for backward.
	 */
	public void travelTo(double[] destination, boolean forward){
		travelTo(destination[0], destination[1], forward);
	}
	
	/**
	 * Sets the target of the navigator, but does not start moving yet.
	 * Use move() to start the navigator.
	 * 
	 * @param x The x coordinate of the target destination.
	 * @param y The y coordinate of the target destination.
	 */
	public void setTarget(double x, double y){
		synchronized (lock) {
			forward = true;
			targetX = x;
			targetY = y;
			targetT = 0.0;
			relativeT = Double.NaN;
			calculateTargetT();
		}
	}
	
	/**
	 * Sets the target of the navigator, but does not start moving yet.
	 * Use move() to start the navigator.
	 * 
	 * @param x The x coordinate of the target destination.
	 * @param y The y coordinate of the target destination.
	 * @param forward True for forward, false for backward.
	 */
	public void setTarget(double x, double y, boolean forward){
		synchronized (lock) {
			this.forward = forward;
			targetX = x;
			targetY = y;
			targetT = 0.0;
			relativeT = Double.NaN;
			calculateTargetT();
		}
	}
	
	/**
	 * Sets the target of the navigator, but does not start moving yet.
	 * Use move() to start the navigator.
	 * 
	 * @param destination The array representing the target destination in the form {x, y}.
	 */
	public void setTarget(double[] destination){
		setTarget(destination[0], destination[1]);
	}
	
	/**
	 * Sets the target of the navigator, but does not start moving yet.
	 * Use move() to start the navigator.
	 * 
	 * @param destination The array representing the target destination in the form {x, y}.
	 * @param forward True for forward, false for backward.
	 */
	public void setTarget(double[] destination, boolean forward){
		setTarget(destination[0], destination[1], forward);
	}
	
	/**
	 * Stops the robot dead in its tracks until either move(), travelTo() or turnTo() is called.
	 * Does not cancel previous target destinations: the robot will keep going to the same destination if the move() method is called.
	 * The navigator will however keep recalculating targetTheta while it is paused.
	 */
	public void pause(){
		synchronized (lock) {
			leftMotor.stop(true);
			rightMotor.stop();
			navigating = false;
			turning = false;
		}
	}
	
	
	/**
	 * Unpauses the navigator if it was previously paused.
	 * Does nothing if the navigator was not paused.
	 */
	public void move(){
		synchronized (lock) {
			if(!Double.isNaN(targetT) || !Double.isNaN(targetX) || !Double.isNaN(relativeT))
				navigating = true;
		}
	}
	
	/**
	 * Returns true if and only if the robot is currently moving via the navigator.
	 * 
	 * @return Whether or not the robot is moving.
	 */
	public boolean isNavigating(){
		return navigating;
	}
	
	
	/**
	 * Returns the x value of the navigator's current destination.
	 * A value of NaN means that there is no destination currently.
	 * 
	 * @return The x value of the current destination.
	 */
	public double getTargetX() {
		double result;
		synchronized (lock) {
			result = targetX;
		}
		return result;
	}

	/**
	 * Returns the y value of the navigator's current destination.
	 * A value of NaN means that there is no destination currently.
	 * 
	 * @return The y value of the current destination.
	 */
	public double getTargetY() {
		double result;
		synchronized (lock) {
			result = targetY;
		}
		return result;
	}

	/**
	 * Returns the angle that the robot is trying to achieve (the angle between itself and its destination).
	 * A value of NaN means that there is no destination currently.
	 * 
	 * @return The angle between the robot and its destination. In rads between 0 and 2pi, counterclockwise from the positive x axis.
	 */
	public double getTargetT() {
		double result;
		synchronized (lock) {
			if(relativeT != Double.NaN){
				result = relativeT + odometer.getTheta();
			}
			else{
				result = targetT;
			}
		}
		return result;
	}
	
	/**
	 * Returns a string representation of the navigator's target which fits the EV3's LCD screen.
	 * 
	 * @return The navigator's target x and y, in string form.
	 */
	public String toString(){
		return Utility.truncate("Target x: " + targetX, 16) + Utility.truncate("Target y: " + targetY, 16);
	}
	
	/**
	 * Waits until the navigator stops navigating.
	 */
	public void waitForStop(){
		while(navigating);
	}
	
	public boolean isTurning(){
		return turning;
	}
	
	public void setSpeed(int speed){
		FORWARD_SPEED = speed;
	}
	
}
