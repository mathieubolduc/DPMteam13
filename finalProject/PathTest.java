package finalProject;

import java.util.Random;

import finalProject.Utility.MutableDouble;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3GyroSensor;

public class PathTest {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3GyroSensor gyroSensor = new EV3GyroSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	
	//constants
	private static final double TRACK = 17.0, WHEEL_RADIUS = 2.09;
	
	
	
	public static void main(String[] args) {
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		MutableDouble time = new MutableDouble(0);
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		Display display = new Display(t, new Object[]{odometer, navigator, time});
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){}
		
		navigator.turnBy(Math.PI*2);
		/*
		//navigate to random locations for 5 min, then return to 0,0
		Random r = new Random();
		long startTime = System.currentTimeMillis();
		while(time.value < 60){
			navigator.travelTo(r.nextDouble()*60, r.nextDouble()*60);
			while(navigator.isNavigating()){
				time.value = (System.currentTimeMillis() - startTime) / 1000;
			}
			//Sound.beep();
		}
		navigator.travelTo(0, 0);
		navigator.waitForStop();
		navigator.turnTo(0);
		navigator.waitForStop();
		Sound.beep();
		*/
	}

}
