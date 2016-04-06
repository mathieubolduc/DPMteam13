package finalProject;

import lejos.hardware.Button;

/**
 * A class containing various useful static methods.
 * It also contains a MutableDouble object, which acts double pointer.
 * 
 * @version 1.0
 * @author Mathieu Bolduc
 */
public abstract class Utility {
	
	/**
	 * A Thread that terminates the program when the exit button is pressed.
	 */
	public static Thread exit = new Thread(new Runnable(){
		public void run(){
			while(Button.waitForAnyPress() != Button.ID_ESCAPE);
			System.exit(0);
		}
	});
	
	/**
	 * Truncates a string. If the string is too small, pads with spaces.
	 * 
	 * @param s The original string.
	 * @param length The desired length.
	 * @return The string s truncated to fit the desired length.
	 */
	public static String truncate(String s, int length){
		if(s.length() >= length){
			s = s.substring(0, length);
		}
		else{
			while(s.length() < length){
				s += " ";
			}
		}
		return s;
	}
	
	
	/**
	 * Calculates the smallest difference between two angles.
	 * 
	 * @param a1 The first angle, in rads.
	 * @param a2 The second angle, in rads.
	 * @return The smallest difference between a1 and a2.
	 * For instance, if a1 = pi/4 and a2 = 3pi/2, the result will be 3pi/4.
	 */
	public static double angleDiff(double a1, double a2){
		a1 = a1%(2*Math.PI);
		a2 = a2%(2*Math.PI);
		double diff = Math.abs(a1 - a2);
		return Math.min(diff, 2*Math.PI - diff);
	}
	
	/**
	 * Reverses an array.
	 * 
	 * @param a The array to be reversed.
	 */
	public static void reverse(Object[] a){
		for(int i = 0; i < a.length / 2; i++){
		    Object temp = a[i];
		    a[i] = a[a.length - i - 1];
		    a[a.length - i - 1] = temp;
		}
	}
	
	/**
	 * A class that emulates a Double, but is mutable.
	 */
	public static class MutableDouble{
		
		/**
		 * The value of the double.
		 */
		public double value;
		
		/**
		 * Constructor for MutableDouble.
		 * @param value Starting value.
		 */
		public MutableDouble(double value){
			this.value = value;
		}
		
		public String toString(){
			return "" + value;
		}
	}
	
}
