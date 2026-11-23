package com.empik.coupons.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

	private final UUID userId;

	public UserNotFoundException(UUID userId) {
		super("User with id " + userId + " does not exist.");
		this.userId = userId;
	}

}
