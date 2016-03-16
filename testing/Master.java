package testing;

import finalProject.Utility;
import lejos.hardware.Button;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Master {
	
	private static final EV3LargeRegulatedMotor masterMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	
	public static void main(String[] args){
		masterMotor.setAcceleration(500);
		Utility.exit.start();
		t.clear();
		while(true){
			int btn = Button.waitForAnyPress();
			if(btn == Button.ID_LEFT){
				t.drawString("Left ", 0, 0);
				masterMotor.rotate(50);
			}
			else if(btn == Button.ID_RIGHT){
				t.drawString("Right", 0, 0);
				masterMotor.rotate(-50);
			}
		}
	}
}
