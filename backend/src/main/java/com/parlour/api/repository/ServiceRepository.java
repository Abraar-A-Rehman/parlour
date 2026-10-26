package com.parlour.api.repository;

import com.parlour.api.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByActiveTrueOrderByCategoryAscNameAsc();

    List<ServiceItem> findAllByOrderByCategoryAscNameAsc();
}
