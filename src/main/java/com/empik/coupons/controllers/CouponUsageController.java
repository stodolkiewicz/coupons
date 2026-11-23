package com.empik.coupons.controllers;

import com.empik.coupons.dtos.CouponUsageResponse;
import com.empik.coupons.dtos.RedeemCouponRequest;
import com.empik.coupons.exceptions.InvalidClientIpAddressException;
import com.empik.coupons.mappers.CouponUsageMapper;
import com.empik.coupons.services.CouponUsageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.InetAddress;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons/{code}/usages")
public class CouponUsageController implements CouponUsageApi {

	private final CouponUsageService couponUsageService;
	private final CouponUsageMapper couponUsageMapper;

	public CouponUsageController(
			CouponUsageService couponUsageService,
			CouponUsageMapper couponUsageMapper
	) {
		this.couponUsageService = couponUsageService;
		this.couponUsageMapper = couponUsageMapper;
	}

	/**
	 * {@link HttpServletRequest#getRemoteAddr()} returns the client IP address.
	 * Behind a trusted proxy, Tomcat takes it from X-Forwarded-For ({@code server.forward-headers-strategy: native}).
	 */
	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CouponUsageResponse redeemCoupon(
			@PathVariable String code,
			@Valid @RequestBody RedeemCouponRequest request,
			HttpServletRequest httpRequest
	) {
		return couponUsageMapper.toResponse(couponUsageService.redeemCouponForUser(
				code,
				request.userId(),
				parseClientIpAddress(httpRequest.getRemoteAddr())
		));
	}

	/**
	 * {@link InetAddress#ofLiteral(String)} parses the address without a DNS lookup.
	 * Tomcat copies X-Forwarded-For from a trusted proxy without checking it, so the value can be malformed.
	 */
	private static InetAddress parseClientIpAddress(String clientIpAddress) {
		try {
			return InetAddress.ofLiteral(clientIpAddress);
		} catch (IllegalArgumentException e) {
			throw new InvalidClientIpAddressException(
					clientIpAddress,
					e
			);
		}
	}

}
