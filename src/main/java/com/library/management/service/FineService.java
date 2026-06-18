package com.library.management.service;

import com.library.management.dto.response.FineResponse;

import java.util.List;

public interface FineService {

    FineResponse getById(Long id);

    List<FineResponse> getMemberFines(Long memberId);

    FineResponse payFine(Long fineId);

    void runDailyFineUpdate();
}
