package testing;

import finalProject.Utility;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

public class Slave {
	
	private static final EV3LargeRegulatedMotor slaveMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	private static final TextLCD t = LocalEV3.get().getTextLCD();
	
	public static void main(String[] args){
		slaveMotor.setAcceleration(500);
		Utility.exit.start();
		slaveMotor.resetTachoCount();
		t.clear();
		while(true){
			while(Math.abs(slaveMotor.getTachoCount()) < 30);
			if(slaveMotor.getTachoCount() > 0){
				t.drawString("Left ", 0, 0);
			}
			else{
				t.drawString("Right", 0, 0);
			}
			slaveMotor.resetTachoCount();
		}
	}
}
