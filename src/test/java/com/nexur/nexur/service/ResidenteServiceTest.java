package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.model.Residente;
import com.nexur.nexur.model.Rol;
import com.nexur.nexur.model.Usuario;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.ResidenteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidenteServiceTest {
    @Mock private ResidenteRepository residenteRepository;
    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private UsuarioService usuarioService;

    @Test
    void listaBuscaEliminaYValidaDatosObligatorios() {
        when(residenteRepository.findAll()).thenReturn(List.of());
        when(residenteRepository.findById(99L)).thenReturn(Optional.empty());
        ResidenteService service = service();
        assertEquals(0, service.obtenerTodos().size());
        assertThrows(RuntimeException.class, () -> service.buscarPorId(99L));
        assertThrows(RuntimeException.class, () -> service.buscarPorUsuarioEmail("none@example.com"));
        service.eliminar(9L);
        verify(residenteRepository).deleteById(9L);
        assertThrows(IllegalArgumentException.class, () -> service.guardar(null, 1L));
        Residente sinDocumento = new Residente();
        assertThrows(IllegalArgumentException.class, () -> service.guardar(sinDocumento, 1L));
    }

    @Test
    void rechazaDocumentoDuplicadoYApartamentoInexistente() {
        Residente residente = new Residente();
        residente.setDocumento("12345678");
        when(residenteRepository.existsByDocumento("12345678")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service().guardar(residente, 1L));
        when(residenteRepository.existsByDocumento("12345678")).thenReturn(false);
        when(apartamentoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service().guardar(residente, 1L));
    }

    @Test
    void guardaResidenteNuevoConUsuarioYRolPorDefecto() {
        Residente residente = residente();
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@example.com");
        residente.setUsuario(usuario);
        when(apartamentoRepository.findById(1L)).thenReturn(Optional.of(residente.getApartamento()));
        when(residenteRepository.existsByDocumento("12345678")).thenReturn(false);
        when(usuarioService.guardarUsuario(usuario)).thenAnswer(invocation -> invocation.getArgument(0));
        when(residenteRepository.save(any(Residente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Residente guardado = service().guardar(residente, 1L);

        assertEquals(Rol.RESIDENTE, usuario.getRol());
        assertEquals("12345678", guardado.getDocumento());
    }

    @Test
    void actualizaResidenteExistenteConUsuarioOSinUsuario() {
        Residente residente = residente();
        residente.setId(2L);
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setEmail("ana@example.com");
        residente.setUsuario(usuario);
        when(apartamentoRepository.findById(1L)).thenReturn(Optional.of(residente.getApartamento()));
        when(residenteRepository.existsByDocumentoAndIdNot("12345678", 2L)).thenReturn(false);
        when(usuarioService.guardarUsuarioActualizado(usuario)).thenReturn(usuario);
        when(residenteRepository.save(any(Residente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(residente, service().guardar(residente, 1L));

        Residente sinUsuario = residente();
        sinUsuario.setId(4L);
        Residente existente = new Residente();
        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setEmail("existente@example.com");
        existente.setUsuario(usuarioExistente);
        when(residenteRepository.existsByDocumentoAndIdNot("12345678", 4L)).thenReturn(false);
        when(residenteRepository.findById(4L)).thenReturn(Optional.of(existente));
        assertEquals(usuarioExistente, service().guardar(sinUsuario, 1L).getUsuario());
    }

    @Test
    void rechazaUsuarioSinCorreoYBuscaPorCorreo() {
        Residente residente = residente();
        residente.setId(5L);
        residente.setUsuario(new Usuario());
        when(apartamentoRepository.findById(1L)).thenReturn(Optional.of(residente.getApartamento()));
        when(residenteRepository.existsByDocumentoAndIdNot("12345678", 5L)).thenReturn(false);
        when(residenteRepository.findById(5L)).thenReturn(Optional.of(residente));
        when(residenteRepository.findByUsuarioEmail("ana@example.com")).thenReturn(Optional.of(residente));
        when(residenteRepository.save(any(Residente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(residente, service().buscarPorUsuarioEmail("ana@example.com"));
        service().guardar(residente, 1L);
    }

    private ResidenteService service() {
        return new ResidenteService(residenteRepository, apartamentoRepository, usuarioService);
    }

    private Residente residente() {
        Residente residente = new Residente();
        residente.setNombre("Ana");
        residente.setDocumento("12345678");
        Apartamento apartamento = new Apartamento();
        apartamento.setId(1L);
        residente.setApartamento(apartamento);
        return residente;
    }
}
