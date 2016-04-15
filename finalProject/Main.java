package finalProject;

import java.io.IOException;
import java.util.Arrays;

import finalProject.Localizer.Type;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import wifi.WifiConnection;

public class Main {
	
	//motors and sensors
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	private static final EV3ColorSensor colorSensor1 = new EV3ColorSensor(LocalEV3.get().getPort("S1"));
	private static final EV3ColorSensor colorSensor2 = new EV3ColorSensor(LocalEV3.get().getPort("S3"));
	private static final EV3UltrasonicSensor usSensor = new EV3UltrasonicSensor(LocalEV3.get().getPort("S2"));
	private static final EV3TouchSensor touchSensor = new EV3TouchSensor(LocalEV3.get().getPort("S4"));
	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final EV3LargeRegulatedMotor launchMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final EV3LargeRegulatedMotor flapsMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	
	//constants
	//robot constants
	private static final double TRACK = 16.45, WHEEL_RADIUS = 2.05, SENSOR_DIST_TANGENT = 16.5;
	
	//field constants
	private static final double SQUARE_LENGTH = 30.67; 
	private static final int MAX_LENGTH = 10;
	
	//ball tray constants
	private static final double X_DIST = -1/*3.81*/;
	
	//wifi constants
	private static final String SERVER_IP = "192.168.10.200";
	private static final int TEAM_NUMBER = 13;
	
	
	public static void main(String[] args) throws IOException{
		
		//wait to start
		Button.waitForAnyPress();
		

		//get wifi stuff
		WifiConnection wifiConnection = new WifiConnection(SERVER_IP, TEAM_NUMBER);
		int DTN = wifiConnection.StartData.get("DTN");
		int OTN = wifiConnection.StartData.get("OTN");
		boolean defense =  DTN == 13;
		double llx =  wifiConnection.StartData.get("ll-x");
		double lly = wifiConnection.StartData.get("ll-y");
		double urx =  wifiConnection.StartData.get("ur-x");
		double ury =  wifiConnection.StartData.get("ur-y");
		int sc =  defense ? wifiConnection.StartData.get("DSC") : wifiConnection.StartData.get("OSC");
		int d1 =  wifiConnection.StartData.get("d1");
		int d2 =  wifiConnection.StartData.get("d2");
		
		
		//instantiate classes
		Odometer odometer = new Odometer(leftMotor, rightMotor, null, TRACK, WHEEL_RADIUS);	//the gyro is null if we dont want to use it
		Navigator navigator = new Navigator(odometer, leftMotor, rightMotor);
		ObstacleAvoider obstacleAvoider = new ObstacleAvoider(navigator, odometer, usSensor);
		Localizer localizer = new Localizer(navigator, odometer, usSensor, colorSensor1, SENSOR_DIST_TANGENT, sc);
		//OdometryCorrection odometryCorrection = new OdometryCorrection(odometer, navigator, colorSensor, null, SENSOR_DIST_TANGENT, 0);
		Launcher launcher = new Launcher(launchMotor, touchSensor);
		Aegis aegis = new Aegis(flapsMotor);
		ColorRecognizer colorRecognizer = new ColorRecognizer(navigator, colorSensor2);
		Display display = new Display(t, new Object[]{odometer, navigator, obstacleAvoider.usFilter});	//set the objects we want to display
		
		
		//start the treads
		Utility.exit.start();
		odometer.start();
		navigator.start();
		display.start();
		try{Thread.sleep(1000);}catch(Exception e){} // wait a bit for the sensors to stabilize
		

		//localize
		localizer.localize(Type.US_AND_LIGHT);
		
		//reach the defense/attack zone
		boolean left = sc == 1 || sc == 4;
		if(sc < 3 && defense){
			navigator.travelTo(left ? SQUARE_LENGTH*0.6 : (MAX_LENGTH-0.6)*SQUARE_LENGTH, SQUARE_LENGTH*0.6);
			navigator.waitForStop();
			navigator.travelTo(left ? SQUARE_LENGTH*0.6 : (MAX_LENGTH-0.6)*SQUARE_LENGTH, (MAX_LENGTH-1)*SQUARE_LENGTH);
			obstacleAvoider.avoid(!left);
		}
		else if(sc > 2 && !defense){
			navigator.travelTo(left ? SQUARE_LENGTH*0.6 : (MAX_LENGTH-0.6)*SQUARE_LENGTH, (MAX_LENGTH-0.6)*SQUARE_LENGTH);
			navigator.waitForStop();
			navigator.travelTo(left ? SQUARE_LENGTH*0.6 : (MAX_LENGTH-0.6)*SQUARE_LENGTH, SQUARE_LENGTH);
			obstacleAvoider.avoid(left);
		}
		
		
		
		//play your role
		//defense
		if(defense){
			
			//move to the center of the defense zone
			navigator.travelTo(MAX_LENGTH*SQUARE_LENGTH/2, (MAX_LENGTH-1)*SQUARE_LENGTH);
			navigator.waitForStop();
			//open the flaps
			navigator.turnTo(Math.PI*1.5);
			try{Thread.sleep(1000);}catch(Exception e){}
			aegis.open();
			System.exit(0);
			
		}
		
		//attack
		else{
			
			//move to the center of the attack zone
			navigator.travelTo(MAX_LENGTH/2*SQUARE_LENGTH, SQUARE_LENGTH);
			navigator.waitForStop();
			localizer.localize(Type.LIGHT);
			
			//calculate the ball locations from wifi data
			double[] endLocation = new double[2]; 	//the location where the robot will grab the balls
			double[] preLocation = new double[2]; 	//the location where the robot will start going forward to grab the ball
			double[] lightLocation = new double[2]; //the location where the robot will light-localize before grabbing
			double[][] avoidLocations = null;		//the locations that the robot traveled to when avoiding
			double[][] avoidLocReversed = null;		//the same locations as avoidLocations, but reversed
			if(llx < urx){
				if(lly < ury){
					//  x   ur
					//  x
					//llx
					endLocation[0] = llx*SQUARE_LENGTH + X_DIST*2;
					endLocation[1] = (lly > MAX_LENGTH/2-2 ? ury : lly)*SQUARE_LENGTH;
					preLocation[0] = llx*SQUARE_LENGTH + X_DIST;
					preLocation[1] = lly > MAX_LENGTH/2-2 ? (lly-0.5)*SQUARE_LENGTH : (ury+0.5)*SQUARE_LENGTH;
					lightLocation[0] = llx*SQUARE_LENGTH;
					lightLocation[1] = lly > MAX_LENGTH/2-2 ? (lly-1)*SQUARE_LENGTH : (ury+1)*SQUARE_LENGTH;
				}
				else{
					//llxxx
					// 
					//     ur
					endLocation[0] = (llx > MAX_LENGTH/2 ? urx : llx)*SQUARE_LENGTH;
					endLocation[1] = lly*SQUARE_LENGTH - X_DIST*2;
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
					endLocation[1] = lly*SQUARE_LENGTH + X_DIST*2;
					preLocation[0] = urx > MAX_LENGTH/2 ? (urx-0.5)*SQUARE_LENGTH : (llx+0.5)*SQUARE_LENGTH;
					preLocation[1] = lly*SQUARE_LENGTH + X_DIST;
					lightLocation[0] = urx > MAX_LENGTH/2 ? (urx-1)*SQUARE_LENGTH : (llx+1)*SQUARE_LENGTH;
					lightLocation[1] = lly*SQUARE_LENGTH;
				}
				else{
					//     xll
					//     x
					//ur   x
					endLocation[0] = llx*SQUARE_LENGTH - X_DIST*2;
					endLocation[1] = (ury > MAX_LENGTH/2-2 ? lly : ury)*SQUARE_LENGTH;
					preLocation[0] = llx*SQUARE_LENGTH - X_DIST;
					preLocation[1] = ury > MAX_LENGTH/2-2 ? (ury-0.5)*SQUARE_LENGTH : (lly+0.5)*SQUARE_LENGTH;
					lightLocation[0] = llx*SQUARE_LENGTH;
					lightLocation[1] = ury > MAX_LENGTH/2-2 ? (ury-1)*SQUARE_LENGTH : (lly+1)*SQUARE_LENGTH;
				}
			}
			
			for(int i=0; i<4; i++){
				
				//go to the pre-grab location
				if(avoidLocations == null){
					navigator.travelTo(lightLocation);
					avoidLocations = obstacleAvoider.avoid(llx > MAX_LENGTH/2);
					avoidLocReversed = Arrays.copyOf(avoidLocations, avoidLocations.length);
					Utility.reverse(avoidLocReversed);
				}
				else{
					for(double[] checkPoint : avoidLocations){
						navigator.travelTo(checkPoint);
						navigator.waitForStop();
						navigator.travelTo(lightLocation);
						navigator.waitForStop();
					}
				}
				localizer.localize(Type.LIGHT);
				navigator.travelTo(preLocation);
				navigator.waitForStop();
				
				//go forward until you see the ball
				navigator.setSpeed(50);
				navigator.travelTo(endLocation);
				colorRecognizer.waitForBall();
				navigator.pause();
				Sound.beep();
				navigator.travelTo(odometer.getX() + 5*Math.cos(odometer.getTheta()), odometer.getY() + 5*Math.sin(odometer.getTheta()));
				navigator.waitForStop();
				launcher.grab();
				
				//go back to the shooting zone and shoot
				navigator.setSpeed(400);
				navigator.travelTo(preLocation, false);
				navigator.waitForStop();
				navigator.travelTo(lightLocation, false);
				navigator.waitForStop();
				for(double[] checkPoint : avoidLocReversed){
					navigator.travelTo(checkPoint);
					navigator.waitForStop();
				}
				navigator.travelTo(MAX_LENGTH/2*SQUARE_LENGTH, SQUARE_LENGTH);
				navigator.waitForStop();
				localizer.localize(Type.LIGHT);
				navigator.turnToward(MAX_LENGTH/2*SQUARE_LENGTH, MAX_LENGTH*SQUARE_LENGTH);
				navigator.waitForStop();
				launcher.shoot();
			}
		}
	}

}
