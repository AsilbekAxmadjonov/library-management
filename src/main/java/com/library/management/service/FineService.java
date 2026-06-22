package com.library.management.service;

import com.library.management.domain.enums.FineStatus;
import com.library.management.dto.response.FineResponse;
import com.library.management.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FineService {

    FineResponse getById(Long id);

    List<FineResponse> getMemberFines(Long memberId);

    PageResponse<FineResponse> getAllFines(FineStatus status, Pageable pageable);

    FineResponse payFine(Long fineId);

    void runDailyFineUpdate();
}
