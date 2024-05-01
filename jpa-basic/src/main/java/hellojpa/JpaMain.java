package hellojpa;

import jakarta.persistence.*;

import java.util.List;

public class JpaMain {
    public static void main(String[] args) {
        /* 애풀리케이션 로딩 시점에 딱 한 개만 만들어져야 함 */
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        /* 트렌잭션 단위로 어떤 동작을 수행할 때 매번 만들어줘야 함 -> 고객의 요청이 들어올 때마다 생성, 스레드 간 공유되서는 안됨 */
        EntityManager em = emf.createEntityManager();
        /* JPA 에서는 이 Transaction 을 관리하는 것이 대단히 중요함 */
        EntityTransaction tx = em.getTransaction();
        tx.begin(); // Transaction 을 시작함

        /* 하나의 Transaction 내에서 처리 */
        try {

            Team team = new Team();
            team.setName("TEAM1");
            em.persist(team);

            Member member = new Member();
            member.setName("ryan");
            member.setTeam(team);
            em.persist(member);

            /* 영속성 컨텍스트의 동작과 함께 생각해보기 */
            em.flush();
            em.clear();

            Member findMember = em.find(Member.class, member.getId());
            List<Member> members = findMember.getTeam().getMembers();

            for (Member m : members) {
                System.out.println("m = " + m.getName());
            }

            tx.commit(); // Transaction 내 변경사항을 커밋함
        } catch (Exception e) {
            tx.rollback(); //  Transaction 내 변경사항을 롤백함
        } finally {
            em.close();
            emf.close();
        }
    }
}
