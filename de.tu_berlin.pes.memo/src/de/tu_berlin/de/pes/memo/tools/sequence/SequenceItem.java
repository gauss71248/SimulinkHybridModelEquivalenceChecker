package de.tu_berlin.de.pes.memo.tools.sequence;

/**
 * @author marcus
 *
 */
public class SequenceItem<T> {

	/**
	 * 
	 */
	private T signal;
	
	/**
	 * 
	 */
	private int value;

	/**
	 * @return the signal
	 */
	public T getSignal() {
		return signal;
	}

	/**
	 * @param signal the signal to set
	 */
	public void setSignal(T signal) {
		this.signal = signal;
	}

	/**
	 * @return the value
	 */
	public int getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	public SequenceItem(T signal, int value) {
		super();
		this.signal = signal;
		this.value = value;
	}

	/**
	 * 
	 */
	public SequenceItem() {
		// TODO Auto-generated constructor stub
	}
	
	public String toString() {
		String result = new String();
		result = this.signal.toString() + " = " + this.value;
		
		return result;
	}
	
}
