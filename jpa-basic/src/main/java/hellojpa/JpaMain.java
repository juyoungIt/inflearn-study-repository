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

        /* 테스트로 작성해볼 코드 들어가는 곳 */

        /* 하나의 Transaction 내에서 처리 */
        try {
            tx.begin(); // Transaction 을 시작함
            tx.commit(); // Transaction 내 변경사항을 커밋함
        } catch (Exception e) {
            tx.rollback(); //  Transaction 내 변경사항을 롤백함
        } finally {
            em.close();
            emf.close();
        }
    }
}
