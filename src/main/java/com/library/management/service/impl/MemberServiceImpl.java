package com.library.management.service.impl;

import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.LoanStatus;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.MemberType;
import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;
import com.library.management.dto.response.PageResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.MemberMapper;
import com.library.management.repository.LoanRepository;
import com.library.management.repository.MemberRepository;
import com.library.management.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final LoanRepository loanRepository;

    @Override
    public MemberResponse create(CreateMemberRequest request) {
        log.info("create member requested: email={}", request.email());
        if (memberRepository.existsByEmail(request.email())) {
            log.warn("Duplicate email rejected: email={}", request.email());
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Email already in use: " + request.email(),
                    HttpStatus.CONFLICT);
        }
        Member member = memberMapper.toEntity(request);
        if (member.getType() == null) member.setType(MemberType.STANDARD);
        Member saved = memberRepository.save(member);
        log.info("Member created: id={} email={}", saved.getId(), saved.getEmail());
        return memberMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getById(Long id) {
        log.debug("getById member: id={}", id);
        return memberMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> getAll(Pageable pageable) {
        log.debug("getAll members: page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return PageResponse.from(memberRepository.findAll(pageable)
                .map(memberMapper::toResponse));
    }

    @Override
    public MemberResponse update(Long id, CreateMemberRequest request) {
        log.info("update member requested: id={}", id);
        Member member = findById(id);

        if (!member.getEmail().equals(request.email())
                && memberRepository.existsByEmail(request.email())) {
            log.warn("Update rejected — email already in use: email={} memberId={}", request.email(), id);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Email already in use: " + request.email(),
                    HttpStatus.CONFLICT);
        }

        if (request.phone() != null
                && !request.phone().equals(member.getPhone())
                && memberRepository.existsByPhone(request.phone())) {
            log.warn("Update rejected — phone already in use: phone={} memberId={}", request.phone(), id);
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                    "Phone number already in use: " + request.phone(),
                    HttpStatus.CONFLICT);
        }

        memberMapper.updateEntity(request, member);
        Member saved = memberRepository.save(member);
        log.info("Member updated: id={} email={}", saved.getId(), saved.getEmail());
        return memberMapper.toResponse(saved);
    }

    @Override
    public MemberResponse block(Long id) {
        log.info("block member requested: id={}", id);
        Member member = findById(id);

        long activeLoans = loanRepository
                .countByMemberIdAndStatus(id, LoanStatus.ACTIVE);
        long overdueLoans = loanRepository
                .countByMemberIdAndStatus(id, LoanStatus.OVERDUE);

        if (activeLoans > 0 || overdueLoans > 0) {
            log.warn("Member being manually blocked while having active/overdue loans: " +
                            "memberId={} activeLoans={} overdueLoans={}",
                    id, activeLoans, overdueLoans);
        }

        member.setStatus(MemberStatus.BLOCKED_MANUALLY);
        log.info("Member manually blocked: id={}", id);
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Override
    public MemberResponse activate(Long id) {
        log.info("activate member requested: id={}", id);
        Member member = findById(id);
        member.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(member);
        log.info("Member activated: id={} email={}", id, member.getEmail());
        return memberMapper.toResponse(member);
    }

    @Override
    public void delete(Long id) {
        log.info("delete member requested: id={}", id);
        memberRepository.delete(findById(id));
        log.info("Member deleted: id={}", id);
    }

    private Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id));
    }
}