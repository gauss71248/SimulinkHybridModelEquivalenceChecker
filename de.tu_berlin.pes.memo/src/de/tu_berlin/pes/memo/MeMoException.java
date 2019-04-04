package de.tu_berlin.pes.memo;

public class MeMoException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String message;

	public MeMoException(String msg) {
		this.message = msg;
	}

	@Override
	public String toString() {
		return "MeMoException: " + message;
	}
}
