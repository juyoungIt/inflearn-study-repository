package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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
import study.querydsl.dto.MemberDTO;
import study.querydsl.dto.QMemberDTO;
import study.querydsl.dto.UserDTO;
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

@Test
@DisplayName("프로젝션 대상이 하나인 경우")
public void simpleProjection() {
    List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .orderBy(member.username.asc())
            .fetch();

    assertThat(result.get(0))
            .isInstanceOf(String.class)
            .isEqualTo("member1");
    assertThat(result.get(1))
            .isInstanceOf(String.class)
            .isEqualTo("member2");
    assertThat(result.get(2))
            .isInstanceOf(String.class)
            .isEqualTo("member3");
    assertThat(result.get(3))
            .isInstanceOf(String.class)
            .isEqualTo("member4");
}

    @Test
    @DisplayName("프로젝션 대상이 둘 이상인 경우 - Tuple")
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0)).isInstanceOf(Tuple.class);
        assertThat(result.get(0).get(member.username)).isEqualTo("member1");
        assertThat(result.get(0).get(member.age)).isEqualTo(10);
        assertThat(result.get(1)).isInstanceOf(Tuple.class);
        assertThat(result.get(1).get(member.username)).isEqualTo("member2");
        assertThat(result.get(1).get(member.age)).isEqualTo(20);
        assertThat(result.get(2)).isInstanceOf(Tuple.class);
        assertThat(result.get(2).get(member.username)).isEqualTo("member3");
        assertThat(result.get(2).get(member.age)).isEqualTo(30);
        assertThat(result.get(3)).isInstanceOf(Tuple.class);
        assertThat(result.get(3).get(member.username)).isEqualTo("member4");
        assertThat(result.get(3).get(member.age)).isEqualTo(40);
    }

    @Test
    @DisplayName("순수 JPA에서 DTO로 조회")
    public void findDTOByJPQL() {
        List<MemberDTO> result = em.createQuery(
                "select new study.querydsl.dto.MemberDTO(m.username, m.age) "
                        + "from Member m "
                        + "order by m.username asc",
                MemberDTO.class).getResultList();

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("setter를 사용해서 projection 필드값 세팅")
    public void findDTOBySetter() {
        List<MemberDTO> result = queryFactory
                .select(Projections.bean(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("field를 사용해서 projection 필드값 세팅")
    public void findDTOByField() {
        List<MemberDTO> result = queryFactory
                .select(Projections.fields(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("생성자를 사용해서 projection 필드값 세팅")
    public void findDTOByConstructor() {
        List<MemberDTO> result = queryFactory
                .select(Projections.constructor(MemberDTO.class,
                        member.username,
                        member.age))
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("DTO 필드와 projection 필드명이 일치하지 않는 경우 세팅")
    public void findByUserDTO() {
        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).getName()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getName()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getName()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getName()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("DTO 필드와 projection 필드명이 일치하지 않는 경우 세팅 - 서브쿼리를 사용할 때")
    public void findByUserDTOWithSubQuery() {
        QMember memberSub = new QMember("memberSub");
        List<UserDTO> result = queryFactory
                .select(Projections.fields(UserDTO.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(member.age.max())
                                        .from(memberSub),
                                "age"
                        )))
                .from(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result.get(0).getName()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getName()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getName()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getName()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("@QueryProjection을 사용해서 projection 필드값을 세팅")
    public void findDTOByQueryProjection() {
        List<MemberDTO> result = queryFactory
                .select(new QMemberDTO(member.username, member.age))
                .from(member)
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("동적쿼리 BooleanBuilder - 이름이 'member1', 나이가 10살인 회원을 찾는다.")
    public void dynamicQueryWithBooleanBuilder1() {
        /* 테스트를 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = "member1";
        Integer ageCond = 10;

        List<Member> result = searchMember1(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("동적쿼리 BooleanBuilder - 이름이 'member1'인 회원을 찾는다.")
    public void dynamicQueryWithBooleanBuilder2() {
        /* 테스트를 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = "member1";
        Integer ageCond = null;

        List<Member> result = searchMember1(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("동적쿼리 BooleanBuilder - 나이가 10세 인 회원을 찾는다.")
    public void dynamicQueryWithBooleanBuilder3() {
        /* 테스트를 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = null;
        Integer ageCond = 10;

        List<Member> result = searchMember1(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("동적쿼리 BooleanBuilder - 조건이 없어 그냥 전체를 조회한다.")
    public void dynamicQueryWithBooleanBuilder4() {
        /* 테스트를 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = null;
        Integer ageCond = null;

        List<Member> result = searchMember1(usernameCond, ageCond);
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if (usernameCond != null) {
            booleanBuilder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            booleanBuilder.and(member.age.eq(ageCond));
        }
        return queryFactory
                .selectFrom(member)
                .where(booleanBuilder)
                .fetch();
    }

    @Test
    @DisplayName("동적쿼리 Where Params - 이름이 'member1', 나이가 10살인 회원을 찾는다.")
    public void dynamicQueryWithWhereParams1() {
        /* 테스트 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = "member1";
        Integer ageCond = 10;

        List<Member> result = searchMember2(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("동적쿼리 Where Params - 이름이 'member1'인 회원을 찾는다.")
    public void dynamicQueryWithWhereParams2() {
        /* 테스트 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = "member1";
        Integer ageCond = null;

        List<Member> result = searchMember2(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("동적쿼리 Where Params - 나이가 10살인 회원을 찾는다.")
    public void dynamicQueryWithWhereParams3() {
        /* 테스트 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = null;
        Integer ageCond = 10;

        List<Member> result = searchMember2(usernameCond, ageCond);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("동적쿼리 Where Params - 조건이 없어 그냥 전체를 조회한다.")
    public void dynamicQueryWithWhereParams4() {
        /* 테스트 목적으로 임시로 세팅하는 데이터 */
        String usernameCond = null;
        Integer ageCond = null;

        List<Member> result = searchMember2(usernameCond, ageCond);
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("member2");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();

    }

    private Predicate usernameEq(String usernameCond) {
        return usernameCond == null
                ? null
                :  member.username.eq(usernameCond);
    }

    private Predicate ageEq(Integer ageCond) {
        return ageCond == null
                ? null
                : member.age.eq(ageCond);
    }

    @Test
    @DisplayName("나이가 28세 미만인 회원에 대해서 이름을 '비회원'으로 변경한다.")
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.asc())
                .fetch();

        assertThat(count).isEqualTo(2); // member1, member2만 영향을 받음
        assertThat(result.get(0).getUsername()).isEqualTo("비회원");
        assertThat(result.get(0).getAge()).isEqualTo(10);
        assertThat(result.get(1).getUsername()).isEqualTo("비회원");
        assertThat(result.get(1).getAge()).isEqualTo(20);
        assertThat(result.get(2).getUsername()).isEqualTo("member3");
        assertThat(result.get(2).getAge()).isEqualTo(30);
        assertThat(result.get(3).getUsername()).isEqualTo("member4");
        assertThat(result.get(3).getAge()).isEqualTo(40);
    }

    @Test
    @DisplayName("모든 회원의 나이를 1살 씩 증가시킨다.")
    public void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        /* 벌크 연산이 돌았으므로 영속성 컨텍스트를 초기화함 */
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.asc())
                .fetch();

        assertThat(count).isEqualTo(4);
        assertThat(result.get(0).getAge()).isEqualTo(11);
        assertThat(result.get(1).getAge()).isEqualTo(21);
        assertThat(result.get(2).getAge()).isEqualTo(31);
        assertThat(result.get(3).getAge()).isEqualTo(41);
    }

    @Test
    @DisplayName("모든 회원의 나이에 2를 곱한다.")
    public void bulkMultiply() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();

        /* 벌크 연산이 돌았으므로 영속성 컨텍스트를 초기화함 */
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.age.asc())
                .fetch();

        assertThat(count).isEqualTo(4);
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.get(1).getAge()).isEqualTo(40);
        assertThat(result.get(2).getAge()).isEqualTo(60);
        assertThat(result.get(3).getAge()).isEqualTo(80);
    }

    @Test
    @DisplayName("나이가 18살 이상인 회원들을 삭제한다.")
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        /* 벌크 연산을 수행했으므로 영속성 컨텍스를 초기화 */
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.asc())
                .fetch();

        assertThat(count).isEqualTo(3);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");
        assertThat(result.get(0).getAge()).isEqualTo(10);
    }

    @Test
    @DisplayName("회원 명에서 member -> M으로 replace 함수를 사용하여 변경한다.")
    public void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"
                ))
                .from(member)
                .orderBy(member.age.asc())
                .fetch();

        assertThat(result.get(0)).isEqualTo("M1");
        assertThat(result.get(1)).isEqualTo("M2");
        assertThat(result.get(2)).isEqualTo("M3");
        assertThat(result.get(3)).isEqualTo("M4");
    }

    @Test
    @DisplayName("회원명이 소문자로 변환 시에도 원래 이름과 동일한 회원을 조회한다.")
    public void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                        Expressions.stringTemplate("function('lower', {0})", member.username))
                )
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo("member1");
        assertThat(result.get(1)).isEqualTo("member2");
        assertThat(result.get(2)).isEqualTo("member3");
        assertThat(result.get(3)).isEqualTo("member4");
    }

    @Test
    @DisplayName("회원명이 소문자로 변환 시에도 원래 이름과 동일한 회원을 조회한다. - Querydsl supports")
    public void sqlFunction3() {
        /* 기존 sqlFunction2의 코드를 다음과 같이 작성할 수 있다. */
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .orderBy(member.username.asc())
                .fetch();

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo("member1");
        assertThat(result.get(1)).isEqualTo("member2");
        assertThat(result.get(2)).isEqualTo("member3");
        assertThat(result.get(3)).isEqualTo("member4");
    }
}