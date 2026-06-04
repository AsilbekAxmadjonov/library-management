package com.library.management.service;

import com.library.management.dto.response.FineResponse;

import java.util.List;

// service/FineService.java
public interface FineService {

    FineResponse getById(Long id);

    List<FineResponse> getMemberFines(Long memberId);

    FineResponse payFine(Long fineId);

    // Called by scheduler — not exposed via REST
    void runDailyFineUpdate();
}
