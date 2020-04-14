package org.ciyam.at.test;

import org.ciyam.at.AtLogger;

public class TestLogger implements AtLogger {

	@Override
	public void error(String message) {
		System.err.println("ERROR: " + message);
	}

	@Override
	public void debug(String message) {
		System.err.println("DEBUG: " + message);
	}

	@Override
	public void echo(String message) {
		System.err.println("ECHO: " + message);
	}

}
