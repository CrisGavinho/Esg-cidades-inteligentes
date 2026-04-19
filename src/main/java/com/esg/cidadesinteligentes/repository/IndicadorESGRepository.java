package com.esg.cidadesinteligentes.repository;

import com.esg.cidadesinteligentes.model.IndicadorESG;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IndicadorESGRepository extends JpaRepository<IndicadorESG, Long> {
    List<IndicadorESG> findByCidade(String cidade);
    List<IndicadorESG> findByCategoria(String categoria);
}
