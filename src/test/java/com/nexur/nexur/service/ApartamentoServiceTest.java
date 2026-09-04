package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApartamentoServiceTest {

    @Mock private ApartamentoRepository apartamentoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReservaRepository reservaRepository;

    @Test
    void generaCodigoAlGuardarApartamentoNuevo() {
        ApartamentoService service = service();
        Apartamento apartamento = new Apartamento();
        apartamento.setNumero("101");

        service.guardarApartamento(apartamento);

        assertNotNull(apartamento.getCodigoRegistro());
        assertEquals(16, apartamento.getCodigoRegistro().length());
        verify(apartamentoRepository).save(apartamento);
    }

    @Test
    void generaCodigoCuandoElCodigoExistenteEstaEnBlanco() {
        Apartamento apartamento = new Apartamento();
        apartamento.setCodigoRegistro(" ");

        service().guardarApartamento(apartamento);

        assertNotNull(apartamento.getCodigoRegistro());
        verify(apartamentoRepository).save(apartamento);
    }

    @Test
    void conservaCodigoExistenteYListaYBusca() {
        Apartamento apartamento = new Apartamento();
        apartamento.setCodigoRegistro("URB-EXISTENTE");
        when(apartamentoRepository.findAll()).thenReturn(List.of(apartamento));
        when(apartamentoRepository.findById(3L)).thenReturn(Optional.of(apartamento));
        ApartamentoService service = service();

        service.guardarApartamento(apartamento);

        assertEquals("URB-EXISTENTE", apartamento.getCodigoRegistro());
        assertEquals(1, service.listarApartamentos().size());
        assertEquals(apartamento, service.obtenerApartamentoPorId(3L));
    }

    @Test
    void devuelveNuloCuandoApartamentoNoExiste() {
        when(apartamentoRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(null, service().obtenerApartamentoPorId(99L));
    }

    @Test
    void rechazaArchivoExcelAusente() {
        assertThrows(IllegalArgumentException.class, () -> service().importarExcel(null));
        MockMultipartFile vacio = new MockMultipartFile("archivo", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service().importarExcel(vacio));
    }

    @Test
    void importaFilasValidasYOmiteVaciasDuplicadas() throws IOException {
        byte[] contenido = excelConFilas();
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "apartamentos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido);
        when(apartamentoRepository.existsByNumero("101")).thenReturn(false);
        when(apartamentoRepository.existsByNumero("102")).thenReturn(true);

        int importados = service().importarExcel(archivo);

        assertEquals(1, importados);
        verify(apartamentoRepository).save(any(Apartamento.class));
    }

    @Test
    void eliminaApartamentoSinRelaciones() {
        when(pagoRepository.countByApartamentoId(1L)).thenReturn(0L);
        when(reservaRepository.countByApartamentoId(1L)).thenReturn(0L);

        service().eliminarApartamento(1L);

        verify(apartamentoRepository).deleteById(1L);
    }

    @Test
    void impideEliminarConPagosYReservas() {
        when(pagoRepository.countByApartamentoId(1L)).thenReturn(2L);
        when(reservaRepository.countByApartamentoId(1L)).thenReturn(1L);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service().eliminarApartamento(1L));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("2 pagos"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("1 reserva"));
        verify(apartamentoRepository, never()).deleteById(1L);
    }

    @Test
    void impideEliminarConSoloPagoYConSoloReservas() {
        when(pagoRepository.countByApartamentoId(2L)).thenReturn(1L);
        when(reservaRepository.countByApartamentoId(2L)).thenReturn(0L);
        RuntimeException soloPago = assertThrows(RuntimeException.class,
                () -> service().eliminarApartamento(2L));
        assertEquals("No se puede eliminar el apartamento porque tiene 1 pago asociado.",
                soloPago.getMessage());

        when(pagoRepository.countByApartamentoId(3L)).thenReturn(0L);
        when(reservaRepository.countByApartamentoId(3L)).thenReturn(2L);
        RuntimeException soloReservas = assertThrows(RuntimeException.class,
                () -> service().eliminarApartamento(3L));
        assertEquals("No se puede eliminar el apartamento porque tiene 2 reservas asociadas.",
                soloReservas.getMessage());
    }

    private ApartamentoService service() {
        return new ApartamentoService(apartamentoRepository, pagoRepository, reservaRepository);
    }

    private byte[] excelConFilas() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var encabezado = sheet.createRow(0);
            encabezado.createCell(0).setCellValue("numero");
            var valida = sheet.createRow(1);
            valida.createCell(0).setCellValue(" 101 ");
            valida.createCell(1).setCellValue("Torre A");
            valida.createCell(2).setCellValue(3);
            valida.createCell(3).setCellValue("DISPONIBLE");
            var duplicada = sheet.createRow(2);
            duplicada.createCell(0).setCellValue("102");
            var vacia = sheet.createRow(3);
            vacia.createCell(0).setCellValue(" ");
            sheet.createRow(4);
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
