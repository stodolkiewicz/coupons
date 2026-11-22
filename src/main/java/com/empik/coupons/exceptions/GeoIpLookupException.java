package com.empik.coupons.exceptions;

import java.net.InetAddress;
import lombok.Getter;

/**
 * Signals that the DB-IP Lite file cannot be read during a country lookup, for example because the file is corrupt.
 */
@Getter
public class GeoIpLookupException extends RuntimeException {

	private final InetAddress ip;

	public GeoIpLookupException(
			InetAddress ip,
			Throwable cause
	) {
		super("Cannot look up the country of " + ip.getHostAddress() + " in the DB-IP Lite file.", cause);
		this.ip = ip;
	}

}
