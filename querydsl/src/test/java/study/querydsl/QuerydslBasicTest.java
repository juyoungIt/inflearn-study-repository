package study.querydsl;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
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

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

    @Test
    @DisplayName("SQL에서 지원하는 다양한 aggregation 함수를 사용할 수 있다.")
    public void aggregation() {
        List<Tuple> results = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        assertThat(results.get(0).get(member.count())).isEqualTo(4);
        assertThat(results.get(0).get(member.age.sum())).isEqualTo(100);
        assertThat(results.get(0).get(member.age.avg())).isEqualTo(25);
        assertThat(results.get(0).get(member.age.max())).isEqualTo(40);
        assertThat(results.get(0).get(member.age.min())).isEqualTo(10);
    }

    @Test
    @DisplayName("각 팀별로 평균 연령을 구한다.")
    public void groupBy() {
        List<Tuple> results = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple tupleA = results.get(0);
        Tuple tupleB = results.get(1);

        assertThat(tupleA.get(team.name)).isEqualTo("teamA");
        assertThat(tupleA.get(member.age.avg())).isEqualTo(15);
        assertThat(tupleB.get(team.name)).isEqualTo("teamB");
        assertThat(tupleB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("팀원의 연령평균이 20 이상인 팀을 구한다.")
    public void groupByWithHaving() {
        List<Tuple> results = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(member.age.avg().gt(20))
                .fetch();

        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0).get(team.name)).isEqualTo("teamB");
        assertThat(results.get(0).get(member.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("teamA에 소속된 모든 회원을 조회한다. - use inner join")
    public void innerJoin() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("teamA에 소속된 모든 회원을 조회한다. - use left join")
    public void leftJoin() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("teamA에 소속된 모든 회원을 조회한다. - use right join")
    public void rightJoin() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .rightJoin(member.team, team)
                .where(member.team.name.eq("teamA"))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    @DisplayName("세타 조인, 회원의 이름이 팀 이름과 같은 경우를 조회한다.")
    public void thetaJoin() {
        /* 해당 케이스를 위해 필요한 추가 데이터를 세팅 */
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) // cartesian product
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    @DisplayName("회원과 팀을 조인하면서 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회한다.")
    public void joinOnFiltering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).get(member).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).get(team).getName()).isEqualTo("teamA");
        assertThat(result.get(1).get(member).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).get(team).getName()).isEqualTo("teamA");
        assertThat(result.get(2).get(member).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).get(team)).isNull();
        assertThat(result.get(3).get(member).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).get(team)).isNull();
    }

    @Test
    @DisplayName("연관관계가 없는 두 엔티티를 외부조인, 회원의 이름이 팀 이름과 같은 경우를 외부조인")
    public void joinOnNoRelation() {
        /* 해당 케이스를 위해 필요한 추가 데이터를 세팅 */
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).get(member).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).get(team)).isNull();
        assertThat(result.get(1).get(member).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).get(team)).isNull();
        assertThat(result.get(2).get(member).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).get(team)).isNull();
        assertThat(result.get(3).get(member).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).get(team)).isNull();
        assertThat(result.get(4).get(member).getUsername()).isEqualTo("teamA");
        assertThat(result.get(4).get(team).getName()).isEqualTo("teamA");
        assertThat(result.get(5).get(member).getUsername()).isEqualTo("teamB");
        assertThat(result.get(5).get(team).getName()).isEqualTo("teamB");
        assertThat(result.get(6).get(member).getUsername()).isEqualTo("teamC");
        assertThat(result.get(6).get(team)).isNull();
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("fetch join이 적용되지 않은 경우")
    public void fetchJoinNo() {
        /* fetch join과 관련된 결과를 잘 확인하기 위해 영속성 컨텍스트를 비움 */
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).isFalse();
    }

    @Test
    @DisplayName("fetch join을 적용한 경우")
    public void fetchJoinUse() {
        /* fetch join과 관련된 결과를 잘 확인하기 위해 영속성 컨텍스트를 비움 */
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // fetch join을 적용한 코드
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        assertThat(emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam())).isTrue();
    }

    @Test
    @DisplayName("나이가 가장 많은 회원을 조회하는 경우")
    public void useSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                                select(memberSub.age.max())
                                        .from(memberSub)
                        )
                )
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    @DisplayName("나이가 평균 이상인 회원을 조회하는 경우")
    public void useSubQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    @DisplayName("나이가 10살 초과인 회원을 조회하는 경우")
    public void useSubQueryIn() {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    @DisplayName("select 문 내에서 서브쿼리를 사용하는 경우")
    public void subQueryInSelect() {
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(
                        member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            assertThat(tuple.get(select(memberSub.age.avg()).from(memberSub))).isEqualTo(25);
        }
    }

    @Test
    @DisplayName("Case가 심플한 경우")
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10세")
                        .when(20).then("20세")
                        .when(30).then("30세")
                        .when(40).then("40세")
                        .otherwise("그 이상"))
                .from(member)
                .orderBy(member.age.asc())
                .fetch();

        assertThat(result.get(0)).isEqualTo("10세");
        assertThat(result.get(1)).isEqualTo("20세");
        assertThat(result.get(2)).isEqualTo("30세");
        assertThat(result.get(3)).isEqualTo("40세");
    }

    @Test
    @DisplayName("Case가 복잡한 경우")
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("어린나이")
                        .when(member.age.between(21, 40)).then("좋을나이")
                        .when(member.age.goe(41)).then("멋진나이")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        assertThat(result.get(0)).isEqualTo("어린나이");
        assertThat(result.get(1)).isEqualTo("어린나이");
        assertThat(result.get(2)).isEqualTo("좋을나이");
        assertThat(result.get(3)).isEqualTo("좋을나이");
    }

    @Test
    @DisplayName("멤버의 이름과 그 옆에 A를 붙여서 함께 반환한다.")
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant('A'))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            assertThat(tuple.get(Expressions.constant('A'))).isEqualTo('A');
        }
    }

    @Test
    @DisplayName("'멤버의 이름_나이'의 형식으로 출력한다.")
    public void concat() {
        List<String> result = queryFactory
                .select(member.username
                        .concat("_")
                        .concat(member.age.stringValue()))
                .from(member)
                .fetch();

        assertThat(result.get(0)).isEqualTo("member1_10");
        assertThat(result.get(1)).isEqualTo("member2_20");
        assertThat(result.get(2)).isEqualTo("member3_30");
        assertThat(result.get(3)).isEqualTo("member4_40");
    }
}
