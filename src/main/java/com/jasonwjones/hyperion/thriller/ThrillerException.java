package com.jasonwjones.hyperion.thriller;

import java.sql.SQLException;

@SuppressWarnings("serial")
public class ThrillerException extends SQLException {

	public ThrillerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ThrillerException(String message) {
		super(message);
	}
	
}
