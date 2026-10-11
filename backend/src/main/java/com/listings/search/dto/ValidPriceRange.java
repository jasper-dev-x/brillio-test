package com.listings.search.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Class-level constraint: {@code minPrice <= maxPrice} when both are present.
 * The violation is reported on the {@code minPrice} field so clients can highlight it.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidPriceRangeValidator.class)
public @interface ValidPriceRange {

    String message() default "minPrice cannot be greater than maxPrice";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
