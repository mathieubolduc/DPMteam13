package finalProject;

import java.io.IOException;
import java.util.HashMap;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3GyroSensor;
import wifi.WifiConnection;

public class Main {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3GyroSensor gyroSensor = new EV3GyroSensor(LocalEV3.get().getPort("S2"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
	//constants
	private static final double TRACK = 17.0, WHEEL_RADIUS = 2.09;
	
	//Wifi Parameters
	private static final String SERVER_IP = "192.168.0.101";//default "localhost", enter IP of server computer
	private static final int TEAM_NUMBER = 13;
	
	public static void main(String[] args) {
		
		//Instantiate WifiConnection object
		WifiConnection conn = null;
		try {
			conn = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		} catch (IOException e){
			t.drawString("Connection Failed", 0, 8);
		}
		
		t.clear();
		
		//data is a HashMap with string elements and integer keys
		//it contains all initial parameters to be used
		if (conn != null){
			HashMap<String,Integer> data = conn.StartData;
			if (t == null) {
				t.drawString("Failed to read transmission", 0, 5);
			} else {
				t.drawString("Transmission read", 0, 5);
				t.drawString(t.toString(), 0, 6);
			}
		} else {
			t.drawString("Connection failed", 0, 5);
		}
		
		//wait to start
		Button.waitForAnyPress();
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);	//the gyro is null if we dont want to use it
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		Display display = new Display(t, new Object[]{odometer, navigator});	//set the objects we want to display
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		
		
		
		// start the code here
		
		
	}

}
