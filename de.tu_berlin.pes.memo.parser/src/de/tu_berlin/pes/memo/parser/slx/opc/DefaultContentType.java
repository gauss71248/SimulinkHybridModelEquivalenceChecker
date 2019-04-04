package de.tu_berlin.pes.memo.parser.slx.opc;

public class DefaultContentType extends ContentType {

	private String extension;

	public DefaultContentType(String contentType, String extension) {
		super(contentType);
		this.extension = extension.intern();
	}

	public String getExtension() {
		return extension;
	}
}