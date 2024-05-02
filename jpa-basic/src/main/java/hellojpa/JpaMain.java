package hellojpa;

import jakarta.persistence.*;

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

            Member member = new Member();
            member.setName("Choonsik");

            em.persist(member);

            em.flush();
            em.clear();

            // Member findMember = em.find(Member.class, member.getId());
            Member findMember = em.getReference(Member.class, member.getId());
            System.out.println("findMember before = " + findMember.getClass()); // Hibernate가 만든 프록시 클래스
            System.out.println("findMember.id = " + findMember.getId());
            System.out.println("findMember.name = " + findMember.getName());
            /* 프록시 객체는 내부에 target entity 값만 업데이트하고, 그 자체가 교체되지는 않는다. */
            System.out.println("findMember after = " + findMember.getClass()); // Hibernate가 만든 프록시 클래스

            /* 타입 비교 시 주의할 점 */
            Member memberA = new Member();
            memberA.setName("Ryan");
            em.persist(memberA);

            Member memberB = new Member();
            memberB.setName("Choonsik");
            em.persist(memberB);

            em.flush();
            em.clear();

            /* 하나는 em.find()로 하나는 em.reference() 로 조회한다 */
            Member memberAEntity = em.find(Member.class, memberA.getId());
            Member memberBProxy = em.getReference(Member.class, memberB.getId());

            System.out.println("memberAEntity == memberBProxy : " + (memberAEntity.getClass() == memberBProxy.getClass()));
            System.out.println("memberBProxy instanceof Member : " + (memberBProxy instanceof Member));

            /* em.find() 도 프록시를 반환할 수 있다. */

            tx.commit(); // Transaction 내 변경사항을 커밋함
        } catch (Exception e) {
            tx.rollback(); //  Transaction 내 변경사항을 롤백함
        } finally {
            em.close();
            emf.close();
        }
    }
}
