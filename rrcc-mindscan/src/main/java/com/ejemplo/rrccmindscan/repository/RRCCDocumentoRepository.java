package com.ejemplo.rrccmindscan.repository;

import com.ejemplo.rrccmindscan.model.RRCCDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RRCCDocumentoRepository extends JpaRepository<RRCCDocumento, Long> {

    @Query("SELECT d FROM RRCCDocumento d ORDER BY d.rrccFechaImportacion DESC, d.rrccNombre")
    List<RRCCDocumento> rrccListarTodos();

    @Query("SELECT COUNT(d) FROM RRCCDocumento d WHERE d.rrccHash = :rrccHash")
    long rrccContarPorHash(@Param("rrccHash") String rrccHash);

    @Query("SELECT COUNT(d) FROM RRCCDocumento d")
    long rrccContarTodos();
}
