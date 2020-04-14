package org.ciyam.at;

public interface AtLoggerFactory {

	AtLogger create(final Class<?> loggerName);

}
