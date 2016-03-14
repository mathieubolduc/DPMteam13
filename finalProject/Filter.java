package finalProject;

import java.util.Arrays;

import lejos.hardware.sensor.SensorMode;

/**
 * The class responsible for filtering sensor data.
 * 
 * @version 1.0
 * @author Mathieu
 *
 */
public class Filter {
	
	private final Type t;
	private final SensorMode s;
	private float[] samples;
	private int index;
	
	/**
	 * The type of filter used. Determines how the filtered data will be calculated from the samples.
	 */
	public enum Type{
		/**
		 * Creates an average filter.
		 */
		AVERAGE,
		
		/**
		 * Creates a median filter.
		 */
		MEDIAN,
		
		/**
		 * Creates a derivative filter.
		 */
		DERIVATIVE
	}
	
	/**
	 * Constructor for Filter.
	 * 
	 * @param t The type of filter used.
	 * @param s The sensor (and mode) that will be filtered.
	 * @param window The amount of samples used to calculate the filtered data.
	 * If the type is DERIVATIVE, the window will be automatically set to 2.
	 */
	public Filter(Type t, SensorMode s, int window){
		this.t= t;
		this.s = s;
		if(t == Type.DERIVATIVE)
			window = 2;
		samples = new float[window];
		for(index=0; index<samples.length; index++){
			s.fetchSample(samples, index);
		}
	}
	
	/**
	 * Adds a new sensor reading to the samples.
	 */
	public void addSample(){
		s.fetchSample(samples, index % samples.length);
		index++;
	}
	
	/**
	 * Calculates the filtered data depending on the type of the filter.
	 * For type AVERAGE, the result will be the average of all the samples.
	 * For type MEDIAN, the result will be the median of all the samples.
	 * For type DERIVATIVE, the result will be the difference between the current and previous sample.
	 * 
	 * @return The filtered data.
	 */
	public double getFilteredData(){
		double result = 0.0;
		
		switch(t){
			case AVERAGE:
				for(float f : samples){
					result += f;
				}
				result /= samples.length;
				break;
			
			case MEDIAN:
				Arrays.sort(samples);
				result = samples[samples.length/2];
				if(samples.length % 2 == 0){
					result = (result + samples[samples.length/2]) / 2;
				}
				break;
				
			case DERIVATIVE:
				result = samples[0] - samples[1];
				if(index == 1)
					result *= -1;
				break;
		}
		
		return result;
	}
}