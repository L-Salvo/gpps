package ing.gpps.repository;

import ing.gpps.entity.users.TutorExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<TutorExterno, Long> {

    Optional<TutorExterno> findByEmail(String email);

    @Query("SELECT t FROM TutorExterno t JOIN t.entidad e WHERE e.cuit = :cuit")
    List<TutorExterno> findAllByCuit(Long cuit);

}
