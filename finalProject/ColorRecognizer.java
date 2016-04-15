package finalProject;

import finalProject.Filter.Type;
import lejos.hardware.sensor.EV3ColorSensor;

/**
 * The class responsible for recognizing the color of the ball
 * 
 * @author Dennis Liu
 * @version 1.0
 */

public class ColorRecognizer {
	
	//member variables
	private Navigator navigator;
	private boolean recognizing = true;
	private final static double RED_R_THRESHOLD = 0.02, RED_G_THRESHOLD = 0.03, RED_B_THRESHOLD = 0.02,
								BLUE_G_THRESHOLD = 0.01, BLUE_B_THRESHOLD = 0.01;
//	private final static int COOLDOWN = 300;
	public Filter rFilter;
	public Filter gFilter;
	public Filter bFilter;
		
	public enum Color{
		/**
		 * Targets red balls.
		 */
		RED,
		
		/**
		 * Targets blue balls.
		 */
		BLUE
	}
	
	public ColorRecognizer(Navigator navigator, EV3ColorSensor colorSensor){
		rFilter = new Filter(Type.RED, colorSensor.getRGBMode(),5);
		gFilter = new Filter(Type.GREEN, colorSensor.getRGBMode(),5);
		bFilter = new Filter(Type.BLUE, colorSensor.getRGBMode(),5);
		this.navigator = navigator;
	}
	
	//recognize a ball's color and perform either grab or navigate to next ball
	public void recognize(Color c){
		while(recognizing){
			switch(c){
			case RED:
				if(checkColor(Color.RED)){
					//grab ball
//					launcher.launch(mode.GRAB);
//					Sound.beep();
				} else {
					//go to next ball
					try{Thread.sleep(100);}catch(Exception e){}
				}
			case BLUE:
				if(checkColor(Color.BLUE)){
					//grab ball
//					launcher.launch(mode.GRAB);
//					Sound.beep();
				} else {
					//go to next ball
					try{Thread.sleep(100);}catch(Exception e){}
				}
			}
		}
	}
	
	private boolean checkColor(Color c){
		rFilter.samples = new float[5*3];
		rFilter.saturateSamples(0,true);
		gFilter.samples = new float[5*3];
		gFilter.saturateSamples(0,true);
		bFilter.samples = new float[5*3];
		bFilter.saturateSamples(0,true);
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
	
	public Color waitForBall(){
		boolean isRed, isBlue;
		while(navigator.isNavigating()){
			isRed = checkColor(Color.RED);
			isBlue = checkColor(Color.BLUE);
			if(isRed || isBlue){
				return isRed ? Color.RED : Color.BLUE;
			}
		}
		return null;
	}
}
