package org.ciyam.at;

import java.util.function.Supplier;

public interface AtLogger {

	void error(final String message);

	void error(final Supplier<String> messageSupplier);

	void debug(final String message);

	void debug(final Supplier<String> messageSupplier);

	void echo(final String message);

	void echo(final Supplier<String> messageSupplier);

}
