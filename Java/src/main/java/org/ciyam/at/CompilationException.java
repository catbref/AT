package org.ciyam.at;

@SuppressWarnings("serial")
public class CompilationException extends ExecutionException {

	public CompilationException() {
	}

	public CompilationException(String message) {
		super(message);
	}

	public CompilationException(Throwable cause) {
		super(cause);
	}

	public CompilationException(String message, Throwable cause) {
		super(message, cause);
	}

}
