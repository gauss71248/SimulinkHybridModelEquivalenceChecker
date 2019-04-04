package de.tu_berlin.pes.memo.parser.slx.opc;

public class Relationship {
	private String id, target, type;

	public Relationship(String id, String target, String type) {
		this.id = id;
		this.target = target;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getTarget() {
		return target;
	}

	public String getType() {
		return type;
	}
}