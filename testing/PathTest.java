package testing;

import finalProject.Aegis;
import finalProject.Display;
import finalProject.Localizer;
import finalProject.Localizer.type;
import finalProject.Navigator;
import finalProject.ObstacleAvoider;
import finalProject.Odometer;
import finalProject.OdometryCorrection;
import finalProject.Utility;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class PathTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launchMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor flapsMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
	//constants
	private static final double TRACK = 15.9, WHEEL_RADIUS = 2.08, SENSOR_DIST_TANGENT = 15, SENSOR_DIST_NORMAL = 5;
	
	
	
	public static void main(String[] args) {
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);	//the gyro is null if we dont want to use it
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, navigator, colorSensor, null, SENSOR_DIST_TANGENT, SENSOR_DIST_NORMAL);
		Display display = new Display(t, new Object[]{odometer, odometryCorrection});	//set the objects we want to display
		Localizer localizer = new Localizer(navigator, odometer, usSensor, colorSensor, 0);
		Aegis aegis = new Aegis(flapsMotor);
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		
		
		
		// start the code here
		localizer.localize(type.US);
		navigator.travelTo(60, 60);
		while(navigator.isTurning());
		obstacleAvoider.avoid(true);
		navigator.turnTo(0);
		navigator.waitForStop();
	}

}
