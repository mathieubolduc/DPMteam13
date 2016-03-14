package testing;

import java.io.IOException;
import finalProject.Display;
import finalProject.Filter;
import finalProject.Utility;
import finalProject.Filter.Type;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.sensor.EV3UltrasonicSensor;

public class SensorTest {
	//motors and sensors
		private static final TextLCD t = LocalEV3.get().getTextLCD();
		private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S3"));
		
		
		public static void main(String[] args) throws IOException {
			
			//wait to start
			Button.waitForAnyPress();
			Sound.beep();
			
			Filter medFilter = new Filter(Type.EMPTY, usSensor.getDistanceMode(), 10);
			Filter avgFilter = new Filter(Type.AVERAGE, usSensor.getDistanceMode(), 10);
			Display display = new Display(t, new Object[]{avgFilter, medFilter});
			SensorRecorder sensorRecorder = new SensorRecorder(new Filter[]{avgFilter,  medFilter}, 100, "Test");
			
			//start the Threads
			Utility.exit.start();
			display.start();
			sensorRecorder.start();
			try{Thread.sleep(1000);}catch(Exception e){}
			
			Button.waitForAnyPress();
			sensorRecorder.stopRecording();
		}
}
