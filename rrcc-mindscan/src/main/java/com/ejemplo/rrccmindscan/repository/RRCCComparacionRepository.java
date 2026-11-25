package com.ejemplo.rrccmindscan.repository;

import com.ejemplo.rrccmindscan.model.RRCCComparacion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RRCCComparacionRepository extends JpaRepository<RRCCComparacion, Long> {

    @Query("SELECT c FROM RRCCComparacion c JOIN FETCH c.rrccDocumentoA JOIN FETCH c.rrccDocumentoB ORDER BY c.rrccFecha DESC")
    List<RRCCComparacion> rrccListarRecientes();

    @Modifying
    @Query("DELETE FROM RRCCComparacion c WHERE c.rrccDocumentoA.rrccId = :rrccId OR c.rrccDocumentoB.rrccId = :rrccId")
    int rrccEliminarDeDocumento(@Param("rrccId") Long rrccId);
}
