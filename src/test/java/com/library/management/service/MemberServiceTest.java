package com.library.management.service;

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
import com.library.management.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService unit tests")
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberMapper     memberMapper;
    @Mock private LoanRepository   loanRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member         member;
    private MemberResponse memberResponse;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1L);
        member.setFirstName("Asilbek");
        member.setLastName("Aliyev");
        member.setEmail("asilbek@example.com");
        member.setPhone("+998901234567");
        member.setStatus(MemberStatus.ACTIVE);
        member.setType(MemberType.STANDARD);

        memberResponse = new MemberResponse(
                1L, "Asilbek", "Aliyev",
                "asilbek@example.com", "+998901234567",
                MemberStatus.ACTIVE, MemberType.STANDARD, null);
    }

    @Test
    @DisplayName("create — happy path: member saved and response returned")
    void create_happyPath_savesMember() {
        CreateMemberRequest request = new CreateMemberRequest(
                "Asilbek", "Aliyev", "asilbek@example.com",
                "+998901234567", MemberType.STANDARD);

        when(memberRepository.existsByEmail("asilbek@example.com")).thenReturn(false);
        when(memberMapper.toEntity(request)).thenReturn(member);
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        MemberResponse result = memberService.create(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("asilbek@example.com");
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("create — duplicate email → DUPLICATE_RESOURCE exception")
    void create_duplicateEmail_throwsException() {
        CreateMemberRequest request = new CreateMemberRequest(
                "Asilbek", "Aliyev", "asilbek@example.com",
                null, MemberType.STANDARD);

        when(memberRepository.existsByEmail("asilbek@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.create(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById — existing id → returns response")
    void getById_exists_returnsResponse() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        MemberResponse result = memberService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById — non-existing id → RESOURCE_NOT_FOUND exception")
    void getById_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getById(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("getAll — returns paged response")
    void getAll_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> page = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findAll(pageable)).thenReturn(page);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        PageResponse<MemberResponse> result = memberService.getAll(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).email()).isEqualTo("asilbek@example.com");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("update — happy path: member updated and saved")
    void update_happyPath_updatesMember() {
        CreateMemberRequest request = new CreateMemberRequest(
                "Asilbek", "Aliyev", "asilbek@example.com",
                "+998901234567", MemberType.STANDARD);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        MemberResponse result = memberService.update(1L, request);

        assertThat(result).isNotNull();
        verify(memberMapper).updateEntity(request, member);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("update — email changed to already used email → DUPLICATE_RESOURCE exception")
    void update_emailAlreadyUsed_throwsException() {
        CreateMemberRequest request = new CreateMemberRequest(
                "Asilbek", "Aliyev", "other@example.com",
                "+998901234567", MemberType.STANDARD);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByEmail("other@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    @DisplayName("update — phone changed to already used phone → DUPLICATE_RESOURCE exception")
    void update_phoneAlreadyUsed_throwsException() {
        CreateMemberRequest request = new CreateMemberRequest(
                "Asilbek", "Aliyev", "asilbek@example.com",
                "+998999999999", MemberType.STANDARD);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.existsByPhone("+998999999999")).thenReturn(true);

        assertThatThrownBy(() -> memberService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    @DisplayName("block — sets status to BLOCKED_MANUALLY")
    void block_setsStatusBlocked() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(0L);
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        memberService.block(1L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.BLOCKED_MANUALLY);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("block — member with active loans: still blocks (warns in log)")
    void block_withActiveLoans_stillBlocks() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(2L);
        when(loanRepository.countByMemberIdAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(0L);
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        memberService.block(1L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.BLOCKED_MANUALLY);
    }

    @Test
    @DisplayName("activate — sets status to ACTIVE")
    void activate_setsStatusActive() {
        member.setStatus(MemberStatus.BLOCKED_MANUALLY);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponse(member)).thenReturn(memberResponse);

        memberService.activate(1L);

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("delete — existing id: repository delete called")
    void delete_existing_deletesMember() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.delete(1L);

        verify(memberRepository).delete(member);
    }

    @Test
    @DisplayName("delete — non-existing id → RESOURCE_NOT_FOUND exception")
    void delete_notFound_throwsException() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.delete(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}