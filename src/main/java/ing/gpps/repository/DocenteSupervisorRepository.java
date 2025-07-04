package ing.gpps.repository;

import ing.gpps.entity.users.DocenteSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocenteSupervisorRepository extends JpaRepository<DocenteSupervisor, String> {
} 