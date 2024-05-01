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

            Movie movie = new Movie();
            movie.setDirector("ryan");
            movie.setDirector("choonsik");
            movie.setName("도도도 춘식이");
            movie.setPrice(10_000);

            em.persist(movie);

            em.flush();
            em.clear();

            Movie findMovie = em.find(Movie.class, movie.getId());

            tx.commit(); // Transaction 내 변경사항을 커밋함
        } catch (Exception e) {
            tx.rollback(); //  Transaction 내 변경사항을 롤백함
        } finally {
            em.close();
            emf.close();
        }
    }
}
