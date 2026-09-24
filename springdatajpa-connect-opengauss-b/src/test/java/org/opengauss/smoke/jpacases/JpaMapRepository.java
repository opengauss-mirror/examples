package org.opengauss.smoke.jpacases;

import org.springframework.data.jpa.repository.JpaRepository;

/** t_jpa_map 的仓储（MappingTest 用）。 */
public interface JpaMapRepository extends JpaRepository<JpaMapEntity, Integer> {
}
