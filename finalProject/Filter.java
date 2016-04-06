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
	public float[] samples;
	private int index;
	private int window;
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
		EMPTY,
		
		/**
		 * Creates a filter for red RGB value.
		 */
		RED,
		
		/**
		 * Creates a filter for green RGB value.
		 */
		GREEN,
		
		/**
		 * Creates a filter for blue RGB value.
		 */
		BLUE
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
		this.window = window;
		if(t == Type.DERIVATIVE)
			this.window = 2;
		if(t == Type.EMPTY)
			this.window = 1;
		//if RGB mode, use saturateRGBsample
		if(t == Type.RED || t == Type.GREEN || t == Type.BLUE){
			samples = new float[this.window*3];
			saturateSamples(0,true);
		} else {
		//if other mode, use saturateSample
			samples = new float[this.window];
			saturateSamples(0,false);
		}
		
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
	 * Adds 3 new RGB sensor readings to the samples.
	 */
	public void addRGBSample(){
		synchronized (lock) {
			s.fetchSample(samples, index % samples.length);
			index+=3; //skips 3 indices instead of 1, since each sample adds 3 elements to array
		}
	}
	
	/**
	 * Fills the entire sample array with new readings separated by a time interval.
	 * 
	 * @param period The time interval between two readings.
	 * @param boolean The flag indicating if RGB samples.
	 */
	public void saturateSamples(int period, boolean RGB){
		for(int i=0; i<window; i++){
			if(!RGB){
				addSample();
			} else {
				addRGBSample();
			}
			try{Thread.sleep(period);}catch(Exception e){}
		}
	}
	
	/**
	 * The generic method to be used for US filter
	 * @param period The time interval between two readings.
	 */
	public void saturateSamples(int period){
		for(int i=0; i<window; i++){
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
				
			case RED:
				for(int j = 0; j<samples.length; j+=3){
					if(samples[j]<1){
						result = samples[j];
						break;
					}
				}
				break;
			
			case GREEN:
				for(int j = 1; j<samples.length; j+=3){
					if(samples[j]<1){
						result = samples[j];
						break;
					}
				}
				break;
				
			case BLUE:
				for(int j = 2; j<samples.length; j+=3){
					if(samples[j]<1){
						result = samples[j];
						break;
					}
				}
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
