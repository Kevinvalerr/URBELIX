package com.nexur.nexur.repository;

import com.nexur.nexur.model.PagoWebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoWebhookEventoRepository extends JpaRepository<PagoWebhookEvento, Long> {
    boolean existsByEventoId(String eventoId);
}
