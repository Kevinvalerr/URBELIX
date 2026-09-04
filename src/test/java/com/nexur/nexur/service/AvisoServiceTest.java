package com.nexur.nexur.service;

import com.nexur.nexur.model.Aviso;
import com.nexur.nexur.repository.AvisoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisoServiceTest {
    @Mock private AvisoRepository avisoRepository;

    @Test
    void listaAvisosAdministrativosYVisibles() {
        when(avisoRepository.findAllByOrderByPublicadoEnDesc()).thenReturn(List.of());
        when(avisoRepository.findVisibles(any(LocalDateTime.class))).thenReturn(List.of());
        AvisoService service = new AvisoService(avisoRepository);
        assertEquals(0, service.listarParaAdministracion().size());
        assertEquals(0, service.listarVisibles().size());
    }

    @Test
    void publicaAvisoNormalizadoConVencimientoFuturo() {
        Aviso aviso = new Aviso();
        aviso.setTitulo("  Mantenimiento  ");
        aviso.setContenido("  Corte de agua  ");
        aviso.setVenceEn(LocalDateTime.now().plusDays(1));
        when(avisoRepository.save(aviso)).thenReturn(aviso);

        Aviso publicado = new AvisoService(avisoRepository).publicar(aviso);

        assertEquals("Mantenimiento", publicado.getTitulo());
        assertEquals("Corte de agua", publicado.getContenido());
        assertEquals(true, publicado.isActivo());
    }

    @Test
    void rechazaAvisoIncompletoOVencido() {
        AvisoService service = new AvisoService(avisoRepository);
        assertThrows(IllegalArgumentException.class, () -> service.publicar(null));
        Aviso sinTitulo = new Aviso();
        sinTitulo.setContenido("Contenido");
        assertThrows(IllegalArgumentException.class, () -> service.publicar(sinTitulo));
        Aviso sinContenido = new Aviso();
        sinContenido.setTitulo("Titulo");
        assertThrows(IllegalArgumentException.class, () -> service.publicar(sinContenido));
        Aviso vencido = new Aviso();
        vencido.setTitulo("Titulo");
        vencido.setContenido("Contenido");
        vencido.setVenceEn(LocalDateTime.now().minusMinutes(1));
        assertThrows(IllegalArgumentException.class, () -> service.publicar(vencido));
    }

    @Test
    void cambiaEstadoOSolicitaAvisoInexistente() {
        Aviso aviso = new Aviso();
        when(avisoRepository.findById(1L)).thenReturn(Optional.of(aviso));
        new AvisoService(avisoRepository).cambiarEstado(1L, false);
        assertEquals(false, aviso.isActivo());
        verify(avisoRepository).save(aviso);
        when(avisoRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> new AvisoService(avisoRepository).cambiarEstado(2L, true));
    }
}
