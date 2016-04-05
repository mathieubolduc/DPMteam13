package finalProject;

import finalProject.Filter.Type;
import finalProject.Launcher.mode;
import lejos.hardware.Sound;
import lejos.hardware.sensor.EV3ColorSensor;

/**
 * The class responsible for recognizing the color of the ball
 * 
 * @author Dennis Liu
 * @version 1.0
 */

public class ColorRecognizer {
	
	//member variables
	private EV3ColorSensor colorSensor;
	private Navigator navigator;
	private Odometer odometer;
	private Launcher launcher;
	private boolean recognizing = true;
	private final static double RED_R_THRESHOLD = 0.02, RED_G_THRESHOLD = 0.03, RED_B_THRESHOLD = 0.02,
								BLUE_G_THRESHOLD = 0.01, BLUE_B_THRESHOLD = 0.01;
	private final static int COOLDOWN = 300;
		
	public enum color{
		/**
		 * Targets red balls.
		 */
		RED,
		
		/**
		 * Targets blue balls.
		 */
		BLUE
	}
	
	public ColorRecognizer(Navigator navigator, Odometer odometer, EV3ColorSensor colorSensor, Launcher launcher){
		this.navigator = navigator;
		this.odometer = odometer;
		this.colorSensor = colorSensor;
		this.launcher = launcher;
	}
	
	//recognize a ball's color and perform either grab or navigate to next ball
	public void recognize(color c){
		while(recognizing){
			switch(c){
			case RED:
				if(checkColor(color.RED)==true){
					//grab ball
//					launcher.launch(mode.GRAB);
					Sound.beep();
				} else {
					//go to next ball
					try{Thread.sleep(100);}catch(Exception e){}
				}
			case BLUE:
				if(checkColor(color.BLUE)==true){
					//grab ball
//					launcher.launch(mode.GRAB);
					Sound.beep();
				} else {
					//go to next ball
					try{Thread.sleep(100);}catch(Exception e){}
				}
			}
		}
	}
	
	private boolean checkColor(color c){
		Filter rFilter = new Filter(Type.RED, colorSensor.getRGBMode(),5);
		Filter gFilter = new Filter(Type.GREEN, colorSensor.getRGBMode(),5);
		Filter bFilter = new Filter(Type.BLUE, colorSensor.getRGBMode(),5);
		switch(c){
		case RED:
			
			if(Math.abs(rFilter.getFilteredData())>=RED_R_THRESHOLD&&
				Math.abs(bFilter.getFilteredData())<=RED_B_THRESHOLD&&
				Math.abs(gFilter.getFilteredData())<=RED_G_THRESHOLD){
				
				return true;
			} else {
				return false;
			}
		case BLUE:
			
			if(Math.abs(bFilter.getFilteredData())>=BLUE_B_THRESHOLD&&
			Math.abs(gFilter.getFilteredData())>=BLUE_G_THRESHOLD){
			
			return true;
			} else {
				return false;
			}
		}
		return false;
	}
}
