package com.empik.coupons.exceptions;

import lombok.Getter;

/**
 * Signals that the client IP address is not a valid IP address,
 * for example because a trusted proxy sent a malformed X-Forwarded-For header.
 */
@Getter
public class InvalidClientIpAddressException extends RuntimeException {

	private final String clientIpAddress;

	public InvalidClientIpAddressException(
			String clientIpAddress,
			Throwable cause
	) {
		super("The client IP address " + clientIpAddress + " is not a valid IP address.", cause);
		this.clientIpAddress = clientIpAddress;
	}

}
