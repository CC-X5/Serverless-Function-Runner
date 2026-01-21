package hskl.cn.serverless.registry.repository;

import hskl.cn.serverless.registry.model.Function;
import hskl.cn.serverless.registry.model.Function.FunctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FunctionRepository extends JpaRepository<Function, UUID> {

    Optional<Function> findByName(String name);

    boolean existsByName(String name);

    List<Function> findByStatus(FunctionStatus status);

    List<Function> findByRuntime(String runtime);
}
