package com.empik.coupons.exceptions;

import java.net.InetAddress;
import lombok.Getter;

/**
 * Signals that the country of the client IP address is unknown, for example for a private address.
 */
@Getter
public class ClientCountryUnknownException extends RuntimeException {

	private final InetAddress clientIp;

	public ClientCountryUnknownException(InetAddress clientIp) {
		super("The country of IP address " + clientIp.getHostAddress() + " cannot be determined.");
		this.clientIp = clientIp;
	}

}
