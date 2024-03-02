package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    @DisplayName("JPQL을 사용하여 member를 조회하는 경우")
    public void startJPQL() {
        String queryString = "select m from Member m where m.username = :username";
        Member member = em.createQuery(queryString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(member.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("alias를 직접 지정하여 사용하는 방식")
    public void startQuerydsl() {
        QMember m = new QMember("m"); // 인자 m은 어떤 QMember 인지 구분하기 위함

        Member member = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(member.getUsername()).isEqualTo("member1");
    }

        @Test
        @DisplayName("사전에 생성된 기본 인스턴스를 사용하는 방식")
        public void startQuerydslWithBasicQType() {
            QMember m = QMember.member;

            Member member = queryFactory
                    .select(m)
                    .from(m)
                    .where(m.username.eq("member1"))
                    .fetchOne();

            assertThat(member.getUsername()).isEqualTo("member1");
        }

    @Test
    @DisplayName("static import를 사용하여 더 간결하게 코드를 작성하는 방식")
    public void startQuerydslWithQTypeStaticImport() {
        // 이 방식으로의 사용을 권장한다.
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("검색조건을 추가하여 이름과 나이를 기준으로 member를 검색하는 경우")
    public void searchMemberByNameAndAge() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("검색조건을 추가 시 parameter를 사용하여 코드를 작성하는 방식")
    public void searchMemberByNameAndAgeWithParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

}
