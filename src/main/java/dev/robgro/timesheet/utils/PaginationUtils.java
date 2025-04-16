package dev.robgro.timesheet.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

public final class PaginationUtils {

    private PaginationUtils() {
    }

    public static Pageable createPageable(String sortBy, String sortDir, int page, int size) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return PageRequest.of(page, size, sort);
    }

    public static void setPaginationAttributes(Model model, Page<?> page, int currentPage, int size) {
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("totalItems", page.getTotalElements());
        model.addAttribute("size", size);
    }

    public static void setPaginationAttributesWithSort(Model model, Page<?> page,
                                                       int currentPage, int size,
                                                       String sortBy, String sortDir) {
        setPaginationAttributes(model, page, currentPage, size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
    }
}
