package testing;

import finalProject.Display;
import finalProject.Filter;
import finalProject.Navigator;
import finalProject.ObstacleAvoider;
import finalProject.Odometer;
import finalProject.Utility;
import finalProject.Filter.Type;
import finalProject.Launcher;
import finalProject.Launcher.mode;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class PathTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launcherMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	
	//constants
	private static final double TRACK = 15.32, WHEEL_RADIUS = 2.09;
	
	
	
	public static void main(String[] args) {
		
		//wait to start
		
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		Display display = new Display(t, new Object[]{odometer, obstacleAvoider.getFilter()});
		Launcher launcher = new Launcher(launcherMotor);
		
		//start the threads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		
		while(true){
			Button.waitForAnyPress();
			launcher.launch(mode.GRAB);
			try{Thread.sleep(500);}catch(Exception e){}
			launcher.launch(mode.SHOOT);
			try{Thread.sleep(500);}catch(Exception e){}
			launcher.launch(mode.RESET);
			try{Thread.sleep(500);}catch(Exception e){}
		}
//		navigator.travelTo(60, 60);
//		obstacleAvoider.avoid();
//		navigator.waitForStop();
//		Sound.beep();
		
		
		/*
		Random r = new Random();
		long time = System.currentTimeMillis();
		while(System.currentTimeMillis() - time < 60000){
			navigator.travelTo(r.nextDouble() * 60, r.nextDouble() * 60);
			navigator.waitForStop();
			Sound.beep();
		}
		navigator.travelTo(0,0);
		navigator.waitForStop();
		navigator.turnTo(0);
		navigator.waitForStop();
		Sound.beep();
		*/
	}

}
