package com.empik.coupons.repositories;

import com.empik.coupons.entities.Coupon;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {
}
