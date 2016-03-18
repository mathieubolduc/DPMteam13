package testing;

import java.io.IOException;
import finalProject.Display;
import finalProject.Filter;
import finalProject.Navigator;
import finalProject.Odometer;
import finalProject.Utility;
import finalProject.Filter.Type;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class SensorTest1234 {
	//motors and sensors
		private static final TextLCD t = LocalEV3.get().getTextLCD();
		private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S3"));
		private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
		private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
		

		//constants
		private static final double TRACK = 17.0, WHEEL_RADIUS = 2.09;
		
		
		public static void main(String[] args) throws IOException {
			
			//wait to start
			Button.waitForAnyPress();
			Sound.beep();
			
			Filter noFilter = new Filter(Type.EMPTY, usSensor.getDistanceMode(), 5);
			Filter avgFilter = new Filter(Type.AVERAGE, usSensor.getDistanceMode(), 5);
			Filter medFilter = new Filter(Type.MEDIAN, usSensor.getDistanceMode(), 5);
			Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);
			Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
			SensorRecorder sensorRecorder = new SensorRecorder(new Filter[]{noFilter, avgFilter,  medFilter}, 50, "UStest");
			
			//start the Threads
			Utility.exit.start();
			sensorRecorder.start();
			odometer.start();
			navigator.start();
			try{Thread.sleep(1000);}catch(Exception e){}
			
			navigator.turnBy(Math.PI*2);
			navigator.waitForStop();
			
			sensorRecorder.stopRecording();
		}
}
