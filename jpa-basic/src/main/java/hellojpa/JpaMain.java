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
            /* Member 정보를 저장 -> DB로 Insert 쿼리 날아감 */
            Member member = new Member();
            member.setId(1L);
            member.setName("ryan");
            em.persist(member);

            /* Member 정보를 조회 -> DB로 Select 쿼리 날아가지 않음 */
            /* -> 동일 Transaction 내에서의 요청이므로 영속성 컨텍스트에서 조회하며, DB에 직접 조회하지 않음 */
            Member findMember = em.find(Member.class, 1L);
            System.out.println("findMember.id : " + findMember.getId());
            System.out.println("findMember.name : " + findMember.getName());

            /* Member 정보를 수정 -> DB로 Update 쿼리 날아감 */
            findMember.setName("Choonsik");

            /* Member 정보를 삭제 */
            em.remove(findMember);

            tx.commit(); // Transaction 내 변경사항을 커밋함
        } catch (Exception e) {
            tx.rollback(); //  Transaction 내 변경사항을 롤백함
        } finally {
            /* Entity Manager는 내부적으로 DB Connection을 물고 동작한다. */
            /* -> 그래서 이를 닫아주는 작업을 하는 것은 대단히 중요하다. */
            em.close();
            emf.close();
        }
    }
}
