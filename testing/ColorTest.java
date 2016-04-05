package testing;

import java.io.IOException;


import finalProject.Launcher.mode;
import finalProject.Localizer.type;
import finalProject.ColorRecognizer.color;
import finalProject.*;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import wifi.WifiConnection;

public class ColorTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	//private static final EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launchMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor flapsMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S3"));
	//constants
	private static final double TRACK = 15.7, WHEEL_RADIUS = 2.05, SENSOR_DIST_TANGENT = 15, SQUARE_LENGTH = 30.67;
	private static final String SERVER_IP = "142.157.179.36";
	private static final int TEAM_NUMBER = 13;
	
	
	public static void main(String[] args) throws IOException{
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);	//the gyro is null if we dont want to use it
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		Localizer localizer = new Localizer(navigator, odometer, usSensor, null, SENSOR_DIST_TANGENT);
		//OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, navigator, colorSensor, null, SENSOR_DIST_TANGENT, 0);
		Launcher launcher = new Launcher(launchMotor);
		Aegis aegis = new Aegis(flapsMotor);
		Display display = new Display(t, new Object[]{odometer, obstacleAvoider.usFilter});	//set the objects we want to display
		ColorRecognizer colorRecognizer = new ColorRecognizer(navigator, odometer, colorSensor, launcher);
		//WifiConnection wifiConnection = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		
		//press once for recognizing red color
		//robot performs grab if color is red
		//robot turns if color is not red
		Button.waitForAnyPress();
		colorRecognizer.recognize(color.RED);
		//press again for recognizing blue color
		//robot performs grab if color is blue
		//robot turns if color is not blue
		Button.waitForAnyPress();
		colorRecognizer.recognize(color.BLUE);
		
	}

}
