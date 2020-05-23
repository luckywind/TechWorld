package com.gitlab.johnjvester.jpaspec.service;

import com.gitlab.johnjvester.jpaspec.domain.Member;
import com.gitlab.johnjvester.jpaspec.repository.MemberRepository;
import com.gitlab.johnjvester.jpaspec.specification.MemberSpecification;
import com.gitlab.johnjvester.jpaspec.web.model.FilterRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MemberService {
    private MemberRepository memberRepository;
    private MemberSpecification memberSpecification;

    public MemberService(@Lazy MemberRepository memberRepository, @Lazy MemberSpecification memberSpecification) {
        this.memberRepository = memberRepository;
        this.memberSpecification = memberSpecification;
    }

    public List<Member> getMembers(FilterRequest filter, String searchString) {
        return memberRepository.findAll(Specification.where(memberSpecification.hasString(searchString)
                .or(memberSpecification.hasClasses(searchString)))
                .and(memberSpecification.getFilter(filter)));
    }

    @Transactional(propagation=Propagation.REQUIRED, readOnly=true)
    public void memberStats() {
        List<Member> members = memberRepository.findAll();

        int activeCount = 0;
        int inactiveCount = 0;
        int registeredForClassesCount = 0;
        int notRegisteredForClassesCount = 0;

        for (Member member : members) {
            if (member.isActive()) {
                activeCount++;

                if (CollectionUtils.isNotEmpty(member.getMemberClasses())) {
                    registeredForClassesCount++;
                } else {
                    notRegisteredForClassesCount++;
                }
            } else {
                inactiveCount++;
            }
        }

        log.info("Member Statics:");
        log.info("==============");
        log.info("Active member count: {}", activeCount);
        log.info(" - Registered for Classes count: {}", registeredForClassesCount);
        log.info(" - Not registered for Classes count: {}", notRegisteredForClassesCount);
        log.info("Inactive member count: {}", inactiveCount);
        log.info("==========================");
    }
}
