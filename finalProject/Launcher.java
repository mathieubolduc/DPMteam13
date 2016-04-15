package finalProject;

import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3TouchSensor;

/**
 * The class responsible for launching the ball
 * 
 * @author Dennis Liu
 * @version 1.0
 */
public class Launcher{
	
	//member variables
	private EV3LargeRegulatedMotor launchMotor;
	private EV3TouchSensor touchSensor;
	
	
	/**
	 * Constructor for Launcher
	 * 
	 * @param launchMotor The robot's motor responsible for launching mechanism.
	 * @param touchSensor The touch sensor that detects when the launching mechanism is rewinded.
	 */
	public Launcher(EV3LargeRegulatedMotor launchMotor, EV3TouchSensor touchSensor){
		this.launchMotor = launchMotor;
		this.touchSensor = touchSensor;
		launchMotor.resetTachoCount();
	}
	
	public void grab(){
		launchMotor.setSpeed(200);
		float[] sample = new float[1];
		launchMotor.backward();
		do{
			touchSensor.getTouchMode().fetchSample(sample, 0);
		} while(sample[0] != 1);
		launchMotor.stop();
	}
	
	public void shoot(){
		launchMotor.setSpeed(700);
		float[] sample = new float[1];
		launchMotor.backward();
		do{
			touchSensor.getTouchMode().fetchSample(sample, 0);
		} while(sample[0] != 0);
		launchMotor.stop();
	}
}
