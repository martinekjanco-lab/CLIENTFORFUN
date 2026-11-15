package dev.vapelike.module;

public class Setting {
	public final String name;
	public final double min, max;
	public double value;

	public Setting(String name, double min, double max, double def) {
		this.name = name;
		this.min = min;
		this.max = max;
		this.value = def;
	}
}
