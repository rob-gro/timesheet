package dev.robgro.timesheet.seller;

import java.util.List;

public interface SellerService {
    List<SellerDto> getAllSellers();
    SellerDto getSellerById(Long id);
    List<SellerDto> searchSellersByName(String name);
    SellerDto saveSeller(SellerDto sellerDto);
    SellerDto createSeller(SellerDto sellerDto);
    SellerDto updateSeller(Long id, SellerDto sellerDto);
    void deleteSeller(Long id);
    OperationResult deactivateSeller(Long id);
    SellerDto createEmptySellerDto();
}