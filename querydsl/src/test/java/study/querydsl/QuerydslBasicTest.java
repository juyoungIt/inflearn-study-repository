package study.querydsl;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
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

import java.util.List;

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

    @Test
    @DisplayName(".fetch()를 사용해서 조회한 목록을 리스트로 가져온다")
    public void searchMembers() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        assertThat(members.size()).isEqualTo(4);
    }

    @Test
    @DisplayName(".fetchOne()을 사용해서 조회한 Entity를 가져온다")
    public void searchMember() {
        assertThatThrownBy(() -> queryFactory
                .selectFrom(member)
                .fetchOne())
                .isInstanceOf(NonUniqueResultException.class);
    }

    @Test
    @DisplayName(".fetchFirst()을 사용해서 목록 중 첫번째만 가져온다")
    public void searchFirstMember() {
        Member firstMember = queryFactory
                .selectFrom(member)
                .fetchFirst();

        assertThat(firstMember).isInstanceOf(Member.class);
    }

    @Test
    @DisplayName(".fetchResults()를 사용해서 페이징 정보와 함께 조회한다.")
    public void searchMemberWithPagingInfo() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getResults()).isInstanceOf(List.class);
        assertThat(results.getResults().size()).isEqualTo(4);
    }

    @Test
    @DisplayName(".fetchCount()를 사용해서 목록의 갯수를 조회한다.")
    public void searchMemberCounts() {
        long counts = queryFactory
                .selectFrom(member)
                .fetchCount();

        assertThat(counts).isEqualTo(4);
    }

    @Test
    @DisplayName("회원 나이별 내림차순, 이름별 오름차순(단, null인 경우 마지막으로) 정렬")
    public void sortByUsernameAndAge() {
        /* 해당 예시를 위해 관련 데이터를 보강하는 로직 */
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(members.get(0).getUsername()).isEqualTo("member5");
        assertThat(members.get(1).getUsername()).isEqualTo("member6");
        assertThat(members.get(2).getUsername()).isNull();
    }

    @Test
    @DisplayName("Paging을 통해 조회 건수를 제한한다.")
    public void paging1() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작, 이는 1개를 생략함을 의미함
                .limit(2)  // 최대 2건을 조회한다
                .fetch();

        assertThat(members.size()).isEqualTo(2);
        assertThat(members.get(0).getUsername()).isEqualTo("member3");
        assertThat(members.get(1).getUsername()).isEqualTo("member2");
    }

    @Test
    @DisplayName("전체 조회 수가 필요한 경우")
    public void paging2() {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

}
