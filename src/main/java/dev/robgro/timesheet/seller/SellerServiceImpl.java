package dev.robgro.timesheet.seller;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final SellerDtoMapper sellerDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SellerDto> getAllSellers() {
        return sellerRepository.findAllActiveOrderByName().stream()
                .map(sellerDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDto> getAllSellers(boolean includeInactive) {
        List<Seller> sellers = includeInactive
                ? sellerRepository.findAllOrderByActiveAndName()
                : sellerRepository.findAllActiveOrderByName();

        return sellers.stream()
                .map(sellerDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDto getSellerById(Long id) {
        return sellerDtoMapper.apply(getSellerOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDto> searchSellersByName(String name) {
        return sellerRepository.findActiveSellersByName(name).stream()
                .map(sellerDtoMapper)
                .collect(Collectors.toList());
    }

    private Seller getSellerOrThrow(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seller", id));
    }

    @Override
    public SellerDto saveSeller(SellerDto sellerDto) {
        if (sellerDto.id() == null) {
            return createSeller(sellerDto);
        } else {
            return updateSeller(sellerDto.id(), sellerDto);
        }
    }

    @Override
    public SellerDto createSeller(SellerDto sellerDto) {
        Seller seller = new Seller();
        seller.setActive(true);
        updateSellerFields(seller, sellerDto);
        return sellerDtoMapper.apply(sellerRepository.save(seller));
    }

    @Override
    public SellerDto updateSeller(Long id, SellerDto sellerDto) {
        Seller seller = getSellerOrThrow(id);
        updateSellerFields(seller, sellerDto);
        return sellerDtoMapper.apply(sellerRepository.save(seller));
    }

    private void updateSellerFields(Seller seller, SellerDto dto) {
        seller.setName(dto.name());
        seller.setStreet(dto.street());
        seller.setPostcode(dto.postcode());
        seller.setCity(dto.city());
        seller.setServiceDescription(dto.serviceDescription());
        seller.setBankName(dto.bankName());
        seller.setAccountNumber(dto.accountNumber());
        seller.setSortCode(dto.sortCode());
        seller.setEmail(dto.email());
        seller.setPhone(dto.phone());
        seller.setCompanyRegistrationNumber(dto.companyRegistrationNumber());
        seller.setLegalForm(dto.legalForm());
        seller.setVatNumber(dto.vatNumber());
        seller.setTaxId(dto.taxId());

        // Handle system default seller logic
        if (dto.systemDefault() && !seller.isSystemDefault()) {
            // Clear system default from all other sellers
            sellerRepository.findByIsSystemDefaultTrue()
                    .ifPresent(currentDefault -> {
                        currentDefault.setSystemDefault(false);
                        sellerRepository.save(currentDefault);
                    });
        }
        seller.setSystemDefault(dto.systemDefault());
    }

    @Transactional
    @Override
    public void deleteSeller(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seller", id));
        seller.setActive(false);
        sellerRepository.save(seller);
        log.info("Seller with id {} has been deactivated", id);
    }

    @Transactional
    @Override
    public OperationResult deactivateSeller(Long id) {
        try {
            Seller seller = getSellerOrThrow(id);
            seller.setActive(false);
            sellerRepository.save(seller);
            log.info("Seller with id {} has been deactivated", id);
            return new OperationResult(true, "Seller has been successfully deactivated");
        } catch (Exception e) {
            log.error("Failed to deactivate seller with id: {}", id, e);
            return new OperationResult(false, "Unable to deactivate seller");
        }
    }

    @Transactional
    @Override
    public OperationResult setActiveStatus(Long id, boolean active) {
        try {
            Seller seller = getSellerOrThrow(id);
            seller.setActive(active);
            sellerRepository.save(seller);

            String action = active ? "reactivated" : "deactivated";
            log.info("Seller with id {} has been {}", id, action);
            return new OperationResult(true,
                    String.format("Seller has been successfully %s", action));
        } catch (Exception e) {
            log.error("Failed to set active status for seller id: {}", id, e);
            return new OperationResult(false, "Unable to update seller status");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDto createEmptySellerDto() {
        return new SellerDto(
                null, "", "", "", "", "",
                null, null, null, null,
                null, null, null, null, null, true, false
        );
    }
}