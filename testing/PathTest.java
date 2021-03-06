package testing;

import java.io.IOException;

import finalProject.Aegis;
import finalProject.Display;
import finalProject.Launcher;
import finalProject.Localizer;
import finalProject.Localizer.Type;
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
import wifi.WifiConnection;

public class PathTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	//private static final EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launchMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor flapsMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
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
		Localizer localizer = new Localizer(navigator, odometer, usSensor, null, SENSOR_DIST_TANGENT, 1);
		//OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, navigator, colorSensor, null, SENSOR_DIST_TANGENT, 0);
		//Launcher launcher = new Launcher(launchMotor);
		Aegis aegis = new Aegis(flapsMotor);
		Display display = new Display(t, new Object[]{odometer, obstacleAvoider.usFilter});	//set the objects we want to display
		//WifiConnection wifiConnection = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		
		//get wifi stuff
		int llx = 90;			//wifiConnection.StartData.get("ll-x");
		int lly = 90;			//wifiConnection.StartData.get("ll-y");
		int urx = 100;			//wifiConnection.StartData.get("ur-x");
		int ury = 120;			//wifiConnection.StartData.get("ur-y");
		int sc = 1;				//wifiConnection.StartData.get("sc");
		
		// start the code here
		
		//localize
		
		//start the odometry correction (must be started after the localization is done)
		//odometryCorrection.start();
		
		//do stuff
		localizer.localize(Type.LIGHT);
		navigator.travelTo(0, 0);
		navigator.waitForStop();
		navigator.turnTo(0);
		navigator.waitForStop();
	}

}
