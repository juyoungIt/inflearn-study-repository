package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    @DisplayName("MemberJpaRepository 의 코드가 기본적으로 잘 동작하는 지 간단하게 확인")
    public void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    @DisplayName("Querydsl로 작성한 코드들이 잘 동작하는 지 간단하게 확인")
    public void basicQuerydslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findByIdByQuerydsl(member.getId());
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAllByQuerydsl();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsernameByQuerydsl("member1");
        assertThat(result2).containsExactly(member);
    }

}