package finalProject;

import java.io.IOException;

import finalProject.Launcher.mode;
import finalProject.Localizer.type;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import wifi.WifiConnection;

public class Main {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3ColorSensor colorSensor = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launchMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor flapsMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
	//constants
	//robot constants
	private static final double TRACK = 15.7, WHEEL_RADIUS = 2.05, SENSOR_DIST_TANGENT = 17.5;
	
	//field constants
	private static final double SQUARE_LENGTH = 30.67; 
	private static final int MAX_LENGTH = 3;
	
	//ball tray constants
	private static final double X_DIST = 3.81;
	
	//wifi constants
	private static final String SERVER_IP = "142.157.179.36";
	private static final int TEAM_NUMBER = 13;
	
	
	public static void main(String[] args) throws IOException{
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);	//the gyro is null if we dont want to use it
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		Localizer localizer = new Localizer(navigator, odometer, usSensor, colorSensor, SENSOR_DIST_TANGENT, 1);
		//OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, navigator, colorSensor, null, SENSOR_DIST_TANGENT, 0);
		Launcher launcher = new Launcher(launchMotor);
		Aegis aegis = new Aegis(flapsMotor);
		Display display = new Display(t, new Object[]{odometer, navigator});	//set the objects we want to display
		//WifiConnection wifiConnection = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		
		//get wifi stuff
		double llx = 2;			//wifiConnection.StartData.get("ll-x");
		double lly = 2;			//wifiConnection.StartData.get("ll-y");
		double urx = 3;			//wifiConnection.StartData.get("ur-x");
		double ury = 3;			//wifiConnection.StartData.get("ur-y");
		int sc = 2;								//wifiConnection.StartData.get("sc");
		
		//calculate the ball locations from wifi data
		double[] endLocation = new double[2]; //the location where the robot will grab the balls
		double[] preLocation = new double[2]; //the location where the robot will start going forward to grab the ball
		double[] lightLocation = new double[2]; //the location where the robot will light-localize before grabbing
		if(llx < urx){
			if(lly < ury){
				//  x   ur
				//  x
				//llx
				endLocation[0] = llx*SQUARE_LENGTH + X_DIST;
				endLocation[1] = (lly > MAX_LENGTH/2 ? ury : lly)*SQUARE_LENGTH;
				preLocation[0] = llx*SQUARE_LENGTH + X_DIST;
				preLocation[1] = lly > MAX_LENGTH/2 ? (lly-0.5)*SQUARE_LENGTH : (ury+0.5)*SQUARE_LENGTH;
				lightLocation[0] = llx*SQUARE_LENGTH;
				lightLocation[1] = lly > MAX_LENGTH/2 ? (lly-1)*SQUARE_LENGTH : (ury+1)*SQUARE_LENGTH;
			}
			else{
				//llxxx
				// 
				//     ur
				endLocation[0] = (llx > MAX_LENGTH/2 ? urx : llx)*SQUARE_LENGTH;
				endLocation[1] = lly*SQUARE_LENGTH - X_DIST;
				preLocation[0] = llx > MAX_LENGTH/2 ? (llx-0.5)*SQUARE_LENGTH : (urx+0.5)*SQUARE_LENGTH;
				preLocation[1] = lly*SQUARE_LENGTH - X_DIST;
				lightLocation[0] = llx > MAX_LENGTH/2 ? (llx-1)*SQUARE_LENGTH : (urx+1)*SQUARE_LENGTH;
				lightLocation[1] = lly*SQUARE_LENGTH;
			}
		}
		else{
			if(lly < ury){
				//ur
				// 
				//  xxxll
				endLocation[0] = (urx > MAX_LENGTH/2 ? llx : urx)*SQUARE_LENGTH;
				endLocation[1] = lly*SQUARE_LENGTH + X_DIST;
				preLocation[0] = urx > MAX_LENGTH/2 ? (urx-0.5)*SQUARE_LENGTH : (llx+0.5)*SQUARE_LENGTH;
				preLocation[1] = lly*SQUARE_LENGTH + X_DIST;
				lightLocation[0] = urx > MAX_LENGTH/2 ? (urx-1)*SQUARE_LENGTH : (llx+1)*SQUARE_LENGTH;
				lightLocation[1] = lly*SQUARE_LENGTH;
			}
			else{
				//     xll
				//     x
				//ur   x
				endLocation[0] = llx*SQUARE_LENGTH - X_DIST;
				endLocation[1] = (ury > MAX_LENGTH/2 ? lly : ury)*SQUARE_LENGTH;
				preLocation[0] = llx*SQUARE_LENGTH - X_DIST;
				preLocation[1] = ury > MAX_LENGTH/2 ? (ury-0.5)*SQUARE_LENGTH : (lly+0.5)*SQUARE_LENGTH;
				lightLocation[0] = llx*SQUARE_LENGTH;
				lightLocation[1] = ury > MAX_LENGTH/2 ? (ury-1)*SQUARE_LENGTH : (lly+1)*SQUARE_LENGTH;
			}
		}
		
		
		// start the code here
		
		//localize
		localizer.localize(type.US);
		
		//start the odometry correction (must be started after the localization is done)
		//odometryCorrection.start();
		
		//go to the pre-grab location
		navigator.travelTo(lightLocation);
		navigator.waitForStop();
		localizer.localize(type.LIGHT);
		navigator.travelTo(preLocation);
		navigator.waitForStop();
		
		//go forward until you see the ball
		navigator.travelTo(endLocation);
		/*
		while(navigator.isNavigating()){
			if(you see a ball){
				navigator.pause();
			}
		}
		*/
		try{Thread.sleep(1000);}catch(Exception e){}
		navigator.pause();
		try{Thread.sleep(1000);}catch(Exception e){}
		launcher.launch(mode.GRAB);
		
		//go back to the shooting zone
		navigator.travelTo(preLocation, false);
		navigator.waitForStop();
		navigator.turnToward(0, 0);
		navigator.waitForStop();
		launcher.launch(mode.SHOOT);
	}

}
