package testing;

import finalProject.Display;
import finalProject.Filter;
import finalProject.Localizer;
import finalProject.Localizer.type;
import finalProject.Navigator;
import finalProject.ObstacleAvoider;
import finalProject.Odometer;
import finalProject.Utility;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class PathTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	//private static final EV3GyroSensor gyroSensor = new EV3GyroSensor(LocalEV3.get().getPort("S2"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	
	//constants
	private static final double TRACK = 17.0, WHEEL_RADIUS = 2.09;
	
	
	
	public static void main(String[] args) {
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		Display display = new Display(t, new Object[]{odometer});
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){}
		
		navigator.travelTo(60, 60);
		obstacleAvoider.avoid();
	}

}
