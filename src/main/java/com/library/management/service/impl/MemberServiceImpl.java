package com.library.management.service.impl;

import com.library.management.domain.entity.Member;
import com.library.management.domain.enums.MemberStatus;
import com.library.management.domain.enums.MemberType;
import com.library.management.dto.request.CreateMemberRequest;
import com.library.management.dto.response.MemberResponse;
import com.library.management.exception.BusinessException;
import com.library.management.exception.ErrorCode;
import com.library.management.mapper.MemberMapper;
import com.library.management.repository.MemberRepository;
import com.library.management.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// service/impl/MemberServiceImpl.java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Override
    public MemberResponse create(CreateMemberRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Email already in use: " + request.email(),
                    HttpStatus.CONFLICT);
        }
        Member member = memberMapper.toEntity(request);
        // type defaults to STANDARD if not provided
        if (member.getType() == null) member.setType(MemberType.STANDARD);
        Member saved = memberRepository.save(member);
        log.info("Member created: id={} email={}", saved.getId(), saved.getEmail());
        return memberMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getById(Long id) {
        return memberMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getAll() {
        return memberRepository.findAll()
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public MemberResponse update(Long id, CreateMemberRequest request) {
        Member member = findById(id);
        // If email is being changed, check it's not taken by someone else
        if (!member.getEmail().equals(request.email())
                && memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Email already in use: " + request.email(),
                    HttpStatus.CONFLICT);
        }
        memberMapper.updateEntity(request, member);
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Override
    public MemberResponse block(Long id) {
        Member member = findById(id);
        member.setStatus(MemberStatus.BLOCKED);
        log.info("Member manually blocked: id={}", id);
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Override
    public MemberResponse activate(Long id) {
        Member member = findById(id);
        member.setStatus(MemberStatus.ACTIVE);
        log.info("Member activated: id={}", id);
        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Override
    public void delete(Long id) {
        memberRepository.delete(findById(id));
        log.info("Member deleted: id={}", id);
    }

    private Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Member", id));
    }
}
