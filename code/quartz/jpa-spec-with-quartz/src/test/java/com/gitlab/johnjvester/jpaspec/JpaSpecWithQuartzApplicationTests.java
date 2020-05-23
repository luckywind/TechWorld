package com.gitlab.johnjvester.jpaspec;

import com.gitlab.johnjvester.jpaspec.domain.MemberClass;
import com.gitlab.johnjvester.jpaspec.domain.Member;
import com.gitlab.johnjvester.jpaspec.repository.ClassRepository;
import com.gitlab.johnjvester.jpaspec.repository.MemberRepository;
import com.gitlab.johnjvester.jpaspec.specification.MemberSpecification;
import com.gitlab.johnjvester.jpaspec.web.model.FilterRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(properties="spring.main.banner-mode=off")
@Transactional
public class JpaSpecWithQuartzApplicationTests {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSpecification memberSpecification;

    @Before
    public void init() {
        memberRepository.deleteAll();
        classRepository.deleteAll();

        MemberClass memberClassWaterPolo = new MemberClass();
        memberClassWaterPolo.setName("Water Polo");
        classRepository.save(memberClassWaterPolo);

        MemberClass memberClassSwimming = new MemberClass();
        memberClassSwimming.setName("Swimming");
        classRepository.save(memberClassSwimming);

        MemberClass memberClassLifting = new MemberClass();
        memberClassLifting.setName("Lifting");
        classRepository.save(memberClassLifting);

        MemberClass memberClassPilates = new MemberClass();
        memberClassPilates.setName("Pilates");
        classRepository.save(memberClassPilates);

        MemberClass memberClassZumba = new MemberClass();
        memberClassZumba.setName("Zumba");
        classRepository.save(memberClassZumba);

        Set<MemberClass> gregSet = new HashSet<>();
        gregSet.add(memberClassWaterPolo);
        gregSet.add(memberClassLifting);

        Member memberGreg = new Member();
        memberGreg.setActive(true);
        memberGreg.setFirstName("Greg");
        memberGreg.setLastName("Brady");
        memberGreg.setInterests("I love to cycle and swim");
        memberGreg.setZipCode("90210");
        memberGreg.setMemberClasses(gregSet);
        memberRepository.save(memberGreg);

        Set<MemberClass> marshaSet = new HashSet<>();
        marshaSet.add(memberClassSwimming);
        marshaSet.add(memberClassZumba);

        Member memberMarsha = new Member();
        memberMarsha.setActive(true);
        memberMarsha.setFirstName("Marsha");
        memberMarsha.setLastName("Brady");
        memberMarsha.setInterests("I love to do zumba and pilates");
        memberMarsha.setZipCode("90211");
        memberMarsha.setMemberClasses(marshaSet);
        memberRepository.save(memberMarsha);

        Set<MemberClass> aliceSet = new HashSet<>();
        aliceSet.add(memberClassSwimming);

        Member memberAlice = new Member();
        memberAlice.setActive(false);
        memberAlice.setFirstName("Alice");
        memberAlice.setLastName("Nelson");
        memberAlice.setInterests("I used to love that belt machine-y thing");
        memberAlice.setZipCode("90201");
        memberAlice.setMemberClasses(aliceSet);
        memberRepository.save(memberAlice);
    }

    @Test
    public void testMembersActive() {
        FilterRequest filter = new FilterRequest();
        filter.setActive(true);

        List<Member> memberList = memberRepository.findAll(memberSpecification.getFilter(filter));

        assertEquals(2, memberList.size());
    }

    @Test
    public void testMembersInZip902() {
        FilterRequest filter = new FilterRequest();
        filter.setZipFilter("902");

        List<Member> memberList = memberRepository.findAll(memberSpecification.getFilter(filter));

        assertEquals(3, memberList.size());
    }

    @Test
    public void testMembersWithSwimClassOrInterest() {
        String searchString = "sWIM";

        List<Member> memberList = memberRepository.findAll(Specification.where(memberSpecification.hasString(searchString)
                .or(memberSpecification.hasClasses(searchString))));

        assertEquals(3, memberList.size());
    }

    @Test
    public void testMembersActiveInZip902WithSwimClassOrInterest() {
        FilterRequest filter = new FilterRequest();
        filter.setActive(true);
        filter.setZipFilter("902");
        String searchString = "sWIM";

        List<Member> memberList = memberRepository.findAll(Specification.where(memberSpecification.hasString(searchString)
                .or(memberSpecification.hasClasses(searchString)))
                .and(memberSpecification.getFilter(filter)));

        assertEquals(2, memberList.size());
    }

}
