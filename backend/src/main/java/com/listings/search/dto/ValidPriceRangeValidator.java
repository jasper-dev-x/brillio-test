package com.listings.search.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPriceRangeValidator implements ConstraintValidator<ValidPriceRange, ListingSearchRequest> {

    @Override
    public boolean isValid(ListingSearchRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getMinPrice() == null || request.getMaxPrice() == null) {
            return true; // nothing to compare; individual field constraints handle the rest
        }
        if (request.getMinPrice().compareTo(request.getMaxPrice()) <= 0) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("minPrice")
                .addConstraintViolation();
        return false;
    }
}
