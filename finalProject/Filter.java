package finalProject;

import java.util.Arrays;

import lejos.robotics.SampleProvider;

/**
 * The class responsible for filtering sensor data.
 * 
 * @version 1.0
 * @author Mathieu
 *
 */
public class Filter {
	
	private final Type t;
	private final SampleProvider s;
	private float[] samples;
	private int index;
	private Object lock;
	
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
		DERIVATIVE,
		
		/**
		 * Creates a filter which copies the sensor value.
		 */
		EMPTY
	}
	
	/**
	 * Constructor for Filter.
	 * 
	 * @param t The type of filter used.
	 * @param s The sensor (and mode) that will be filtered.
	 * @param window The amount of samples used to calculate the filtered data.
	 * If the type is DERIVATIVE, the window will be automatically set to 2. If the type is EMPTY, the window will be set to 1.
	 */
	public Filter(Type t, SampleProvider s, int window){
		lock = new Object();
		this.t= t;
		this.s = s;
		if(t == Type.DERIVATIVE)
			window = 2;
		if(t == Type.EMPTY)
			window = 1;
		samples = new float[window];
		saturateSamples(0);
	}
	
	/**
	 * Adds a new sensor reading to the samples.
	 */
	public void addSample(){
		synchronized (lock) {
			s.fetchSample(samples, index % samples.length);
			index++;
		}
	}
	
	/**
	 * Fills the entire sample array with new readings separated by a time interval.
	 * 
	 * @param period The time interval between two readings.
	 */
	public void saturateSamples(int period){
		for(int i=0; i<samples.length; i++){
			addSample();
			try{Thread.sleep(period);}catch(Exception e){}
		}
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
		synchronized (lock) {
			switch(t){
			case AVERAGE:
				int i=0;
				for(float f : samples){
					if(f != Float.POSITIVE_INFINITY){
						result += f;
						i++;
					}
				}
				result = (i == 0) ? Float.POSITIVE_INFINITY : result / i;
				break;
			
			case MEDIAN:
				float[] sorted = Arrays.copyOf(samples, samples.length);
				Arrays.sort(sorted);
				result = sorted[sorted.length/2];
				if(sorted.length % 2 == 0){
					result = (result + sorted[sorted.length/2]) / 2;
				}
				break;
				
			case DERIVATIVE:
				result = samples[0] - samples[1];
				if(index == 1)
					result *= -1;
				break;
				
			case EMPTY:
				result = samples[0];
				break;
			}
		}
		
		return result;
	}

	/**
	 * Returns a string representation of the filter's current value which fits the EV3's LCD screen.
	 * 
	 * @return The filter's current value, in string form.
	 */
	public String toString(){
		return Utility.truncate(t.toString() + ": " + this.getFilteredData(), 16);
	}
}
