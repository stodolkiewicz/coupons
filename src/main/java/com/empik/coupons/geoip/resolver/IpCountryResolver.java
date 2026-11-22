package com.empik.coupons.geoip.resolver;

import java.net.InetAddress;
import java.util.Optional;

public interface IpCountryResolver {

	/**
	 * Finds the country of an IP address.
	 *
	 * @return the ISO 3166-1 alpha-2 country code in uppercase,
	 * or an empty {@link Optional} when the country is unknown
	 */
	Optional<String> resolveCountryCode(InetAddress ip);

}
