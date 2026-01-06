package dev.robgro.timesheet.seller;

import org.springframework.stereotype.Service;
import java.util.function.Function;

@Service
public class SellerDtoMapper implements Function<Seller, SellerDto> {

    @Override
    public SellerDto apply(Seller seller) {
        return new SellerDto(
            seller.getId(),
            seller.getName(),
            seller.getStreet(),
            seller.getPostcode(),
            seller.getCity(),
            seller.getServiceDescription(),
            seller.getBankName(),
            seller.getAccountNumber(),
            seller.getSortCode(),
            seller.getEmail(),
            seller.getPhone(),
            seller.getCompanyRegistrationNumber(),
            seller.getLegalForm(),
            seller.getVatNumber(),
            seller.getTaxId(),
            seller.isActive(),
            seller.isSystemDefault()
        );
    }
}