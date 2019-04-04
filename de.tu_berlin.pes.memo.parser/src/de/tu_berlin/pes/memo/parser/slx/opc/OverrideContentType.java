package de.tu_berlin.pes.memo.parser.slx.opc;

public class OverrideContentType extends ContentType {

	private String partName;

	public OverrideContentType(String contentType, String partName) {
		super(contentType);
		this.partName = partName;
	}

	public String getPartName() {
		return partName;
	}
}
