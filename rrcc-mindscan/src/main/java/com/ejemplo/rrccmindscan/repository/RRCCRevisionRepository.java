package com.ejemplo.rrccmindscan.repository;

import com.ejemplo.rrccmindscan.model.RRCCRevision;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RRCCRevisionRepository extends JpaRepository<RRCCRevision, Long> {

    @Query("SELECT r FROM RRCCRevision r JOIN FETCH r.rrccDocumento ORDER BY r.rrccFecha DESC")
    List<RRCCRevision> rrccListarRecientes();

    @Modifying
    @Query("DELETE FROM RRCCRevision r WHERE r.rrccDocumento.rrccId = :rrccId")
    int rrccEliminarDeDocumento(@Param("rrccId") Long rrccId);
}
