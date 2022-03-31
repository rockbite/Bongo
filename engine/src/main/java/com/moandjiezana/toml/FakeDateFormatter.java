package com.moandjiezana.toml;

public class FakeDateFormatter {
	public FakeDateFormatter (String format) {

	}

	public String parse (String dateString) {
		return dateString;
	}

	public String format (Object value) {
		return value.toString();
	}

	public void setTimeZone (FaketimeZone timeZone) {

	}
}
