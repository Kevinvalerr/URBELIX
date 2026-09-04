package com.nexur.nexur.service;

import com.nexur.nexur.model.Apartamento;
import com.nexur.nexur.repository.ApartamentoRepository;
import com.nexur.nexur.repository.PagoRepository;
import com.nexur.nexur.repository.ReservaRepository;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;

@Service

public class ApartamentoService {

    private final ApartamentoRepository apartamentoRepository;
    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;

    public ApartamentoService(ApartamentoRepository apartamentoRepository,
                              PagoRepository pagoRepository,
                              ReservaRepository reservaRepository){
        this.apartamentoRepository = apartamentoRepository;
        this.pagoRepository = pagoRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Apartamento> listarApartamentos(){
        return apartamentoRepository.findAll();
    }

    public void guardarApartamento(Apartamento apartamento){
        if (apartamento.getCodigoRegistro() == null || apartamento.getCodigoRegistro().isBlank()) {
            apartamento.setCodigoRegistro(generarCodigoRegistro());
        }
        apartamentoRepository.save(apartamento);
    }

    private String generarCodigoRegistro() {
        return "URB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    public int importarExcel(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Seleccione un archivo Excel");
        }
        int importados = 0;
        try (var workbook = WorkbookFactory.create(archivo.getInputStream())) {
            var sheet = workbook.getSheetAt(0);
            for (int indice = 1; indice <= sheet.getLastRowNum(); indice++) {
                Row row = sheet.getRow(indice);
                if (row == null || row.getCell(0) == null) {
                    continue;
                }
                String numero = row.getCell(0).toString().trim();
                if (numero.isBlank() || apartamentoRepository.existsByNumero(numero)) {
                    continue;
                }
                Apartamento apartamento = new Apartamento();
                apartamento.setNumero(numero);
                apartamento.setTorre(row.getCell(1) == null ? "" : row.getCell(1).toString().trim());
                if (row.getCell(2) != null) {
                    apartamento.setPiso((int) row.getCell(2).getNumericCellValue());
                }
                apartamento.setEstado(row.getCell(3) == null ? "DISPONIBLE" : row.getCell(3).toString().trim());
                apartamentoRepository.save(apartamento);
                importados++;
            }
        }
        return importados;
    }

    public Apartamento obtenerApartamentoPorId(Long id){
        return apartamentoRepository.findById(id).orElse(null);
    }

    public void eliminarApartamento(Long id){
        long pagosAsociados = pagoRepository.countByApartamentoId(id);
        long reservasAsociadas = reservaRepository.countByApartamentoId(id);

        if (pagosAsociados > 0 || reservasAsociadas > 0) {
            String mensaje = "No se puede eliminar el apartamento porque tiene ";
            if (pagosAsociados > 0) {
                mensaje += pagosAsociados + " pago" + (pagosAsociados > 1 ? "s" : "") + " asociado" + (pagosAsociados > 1 ? "s" : "");
            }
            if (pagosAsociados > 0 && reservasAsociadas > 0) {
                mensaje += " y ";
            }
            if (reservasAsociadas > 0) {
                mensaje += reservasAsociadas + " reserva" + (reservasAsociadas > 1 ? "s" : "") + " asociada" + (reservasAsociadas > 1 ? "s" : "");
            }
            mensaje += ".";
            throw new RuntimeException(mensaje);
        }
        apartamentoRepository.deleteById(id);
    }

}
