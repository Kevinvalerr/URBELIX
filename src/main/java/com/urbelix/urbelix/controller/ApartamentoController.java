package com.urbelix.urbelix.controller;

import com.urbelix.urbelix.model.Apartamento;
import com.urbelix.urbelix.service.ApartamentoService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.ui.Model;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.urbelix.urbelix.model.EstadoApartamento;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ApartamentoController {
    @Autowired
    private ApartamentoService apartamentoService;


   @GetMapping("/apartamentos") 

   public String listarApartamentos(Model model) {

   List<Apartamento> apartamentos = apartamentoService.listarApartamentos();

    model.addAttribute("apartamentos", apartamentos);
    model.addAttribute("titulo", "Apartamentos");
    model.addAttribute("currentPath", "/apartamentos");
    model.addAttribute("volverUrl", "/dashboard");

    return "apartamentos/lista";

   }

   @GetMapping("/apartamentos/plantilla")
   public ResponseEntity<byte[]> descargarPlantilla() throws IOException {
       try (XSSFWorkbook workbook = new XSSFWorkbook()) {
           Sheet sheet = workbook.createSheet("Apartamentos");
           sheet.setDisplayGridlines(false);
           String[] columns = {"Numero", "Torre", "Piso", "Estado"};

           CellStyle titleStyle = workbook.createCellStyle();
           Font titleFont = workbook.createFont();
           titleFont.setBold(true);
           titleFont.setFontHeightInPoints((short) 16);
           titleFont.setColor(IndexedColors.WHITE.getIndex());
           titleStyle.setFont(titleFont);
           titleStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
           titleStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
           titleStyle.setAlignment(HorizontalAlignment.CENTER);
           titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

           CellStyle headerStyle = workbook.createCellStyle();
           Font headerFont = workbook.createFont();
           headerFont.setBold(true);
           headerFont.setColor(IndexedColors.WHITE.getIndex());
           headerStyle.setFont(headerFont);
           headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
           headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
           headerStyle.setBorderTop(BorderStyle.THIN);
           headerStyle.setBorderBottom(BorderStyle.THIN);
           headerStyle.setAlignment(HorizontalAlignment.CENTER);
           headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

           CellStyle inputStyle = workbook.createCellStyle();
           inputStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
           inputStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
           inputStyle.setBorderTop(BorderStyle.THIN);
           inputStyle.setBorderBottom(BorderStyle.THIN);
           inputStyle.setBorderLeft(BorderStyle.THIN);
           inputStyle.setBorderRight(BorderStyle.THIN);
           inputStyle.setVerticalAlignment(VerticalAlignment.CENTER);

           Row title = sheet.createRow(0);
           title.createCell(0).setCellValue("PLANTILLA DE IMPORTACIÓN DE APARTAMENTOS - URBELIX");
           title.getCell(0).setCellStyle(titleStyle);
           sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columns.length - 1));
           title.setHeightInPoints(28);

           Row header = sheet.createRow(1);
           for (int index = 0; index < columns.length; index++) {
               header.createCell(index).setCellValue(columns[index]);
               header.getCell(index).setCellStyle(headerStyle);
           }

           Row firstDataRow = sheet.createRow(2);
           for (int index = 0; index < columns.length; index++) {
               firstDataRow.createCell(index).setCellValue("");
               firstDataRow.getCell(index).setCellStyle(inputStyle);
           }

           sheet.createFreezePane(0, 2);
           sheet.setAutoFilter(new CellRangeAddress(1, 2, 0, columns.length - 1));
           XSSFTable table = ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet)
               .createTable(new AreaReference("A2:D3", SpreadsheetVersion.EXCEL2007));
           table.setName("ApartamentosImportacion");
           table.setDisplayName("ApartamentosImportacion");
           if (!table.getCTTable().isSetTableStyleInfo()) {
               table.getCTTable().addNewTableStyleInfo();
           }
           table.getCTTable().getTableStyleInfo().setName("TableStyleMedium2");
           table.getCTTable().getTableStyleInfo().setShowRowStripes(true);
           table.getCTTable().getTableStyleInfo().setShowColumnStripes(false);

           DataValidationHelper validationHelper = sheet.getDataValidationHelper();
           DataValidationConstraint numberConstraint = validationHelper.createIntegerConstraint(
               DataValidationConstraint.OperatorType.BETWEEN, "1", "999999999");
           DataValidation numberValidation = validationHelper.createValidation(
               numberConstraint, new CellRangeAddressList(2, 1001, 0, 0));
           numberValidation.setEmptyCellAllowed(false);
           numberValidation.setShowErrorBox(true);
           numberValidation.createErrorBox("Número inválido", "Ingresa un número entero positivo. Este valor es obligatorio.");
           numberValidation.setShowPromptBox(true);
           numberValidation.createPromptBox("Número de apartamento", "Ingresa el número del apartamento. El ID se genera automáticamente en el sistema.");
           sheet.addValidationData(numberValidation);

           DataValidationConstraint floorConstraint = validationHelper.createIntegerConstraint(
               DataValidationConstraint.OperatorType.BETWEEN, "0", "1000");
           DataValidation floorValidation = validationHelper.createValidation(
               floorConstraint, new CellRangeAddressList(2, 1001, 2, 2));
           floorValidation.setEmptyCellAllowed(false);
           floorValidation.setShowErrorBox(true);
           floorValidation.createErrorBox("Piso inválido", "Ingresa un número entero entre 0 y 1000.");
           floorValidation.setShowPromptBox(true);
           floorValidation.createPromptBox("Piso", "Ingresa únicamente un número de piso no negativo.");
           sheet.addValidationData(floorValidation);

           String[] statuses = java.util.Arrays.stream(EstadoApartamento.values())
               .map(Enum::name)
               .toArray(String[]::new);
           DataValidationConstraint statusConstraint = validationHelper.createExplicitListConstraint(statuses);
           DataValidation statusValidation = validationHelper.createValidation(
               statusConstraint, new CellRangeAddressList(2, 1001, 3, 3));
           statusValidation.setEmptyCellAllowed(false);
           statusValidation.setShowErrorBox(true);
           statusValidation.createErrorBox("Estado inválido", "Selecciona un estado de la lista.");
           statusValidation.setShowPromptBox(true);
           statusValidation.createPromptBox("Estado", "Selecciona DISPONIBLE, OCUPADO o MANTENIMIENTO.");
           sheet.addValidationData(statusValidation);

           DataValidationConstraint towerConstraint = validationHelper.createTextLengthConstraint(
               DataValidationConstraint.OperatorType.BETWEEN, "1", "30");
           DataValidation towerValidation = validationHelper.createValidation(
               towerConstraint, new CellRangeAddressList(2, 1001, 1, 1));
           towerValidation.setEmptyCellAllowed(false);
           towerValidation.setShowErrorBox(true);
           towerValidation.createErrorBox("Torre inválida", "Ingresa el identificador de la torre, sin espacios al inicio o al final.");
           towerValidation.setShowPromptBox(true);
           towerValidation.createPromptBox("Torre", "Ejemplo: A, B o Torre 1.");
           sheet.addValidationData(towerValidation);

           for (int index = 0; index < columns.length; index++) {
               sheet.autoSizeColumn(index);
               int width = Math.max(sheet.getColumnWidth(index) + 2 * 256, 12 * 256);
               sheet.setColumnWidth(index, Math.min(width, 32 * 256));
           }

           ByteArrayOutputStream output = new ByteArrayOutputStream();
           workbook.write(output);
           HttpHeaders headers = new HttpHeaders();
           headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
           headers.setContentDisposition(ContentDisposition.attachment().filename("plantilla_apartamentos.xlsx").build());
           return ResponseEntity.ok().headers(headers).body(output.toByteArray());
       }
   }

   @PostMapping("/apartamentos/importar")
   public String importarApartamentos(@RequestParam("archivo") MultipartFile archivo,
                                      RedirectAttributes redirectAttributes) {
       if (archivo.isEmpty() || !archivo.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
           redirectAttributes.addFlashAttribute("error", "Selecciona un archivo Excel .xlsx válido.");
           return "redirect:/apartamentos";
       }

       int importados = 0;
       int omitidos = 0;
       List<String> errores = new java.util.ArrayList<>();
       DataFormatter formatter = new DataFormatter();

       try (XSSFWorkbook workbook = new XSSFWorkbook(archivo.getInputStream())) {
           Sheet sheet = workbook.getSheetAt(0);
           if (sheet.getPhysicalNumberOfRows() == 0) {
               redirectAttributes.addFlashAttribute("error", "El archivo no contiene filas.");
               return "redirect:/apartamentos";
           }

           Row header = sheet.getRow(1);
           if (header == null || !"numero".equalsIgnoreCase(formatter.formatCellValue(header.getCell(0)).trim())
                   || !"torre".equalsIgnoreCase(formatter.formatCellValue(header.getCell(1)).trim())
                   || !"piso".equalsIgnoreCase(formatter.formatCellValue(header.getCell(2)).trim())
                   || !"estado".equalsIgnoreCase(formatter.formatCellValue(header.getCell(3)).trim())) {
               redirectAttributes.addFlashAttribute("error", "La plantilla debe tener las columnas: Numero, Torre, Piso y Estado.");
               return "redirect:/apartamentos";
           }

           for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
               Row row = sheet.getRow(rowIndex);
               String numero = cell(formatter, row, 0);
               String torre = cell(formatter, row, 1);
               String pisoTexto = cell(formatter, row, 2);
               String estado = cell(formatter, row, 3);

               if (numero.isBlank() && torre.isBlank() && pisoTexto.isBlank() && estado.isBlank()) {
                   continue;
               }
               try {
                   if (numero.isBlank() || torre.isBlank() || pisoTexto.isBlank()) {
                       throw new IllegalArgumentException("Numero, Torre y Piso son obligatorios");
                   }
                   int numeroEntero = Integer.parseInt(numero);
                   if (numeroEntero <= 0 || apartamentoService.existePorNumero(numero)) {
                       throw new IllegalArgumentException("El número de apartamento no es válido o ya existe");
                   }
                   torre = torre.trim();
                   int piso = Integer.parseInt(pisoTexto);
                   if (piso < 0) {
                       throw new IllegalArgumentException("El piso no puede ser negativo");
                   }
                   if (!estado.isBlank()) {
                       EstadoApartamento.valueOf(estado.toUpperCase(java.util.Locale.ROOT));
                   }

                   Apartamento apartamento = new Apartamento();
                   apartamento.setNumero(numero);
                   apartamento.setTorre(torre);
                   apartamento.setPiso(piso);
                   apartamento.setEstado(estado.isBlank() ? EstadoApartamento.DISPONIBLE.name() : estado.toUpperCase(java.util.Locale.ROOT));
                   apartamentoService.guardarApartamento(apartamento);
                   importados++;
               } catch (RuntimeException ex) {
                   omitidos++;
                   errores.add("Fila " + (rowIndex + 1) + ": " + ex.getMessage());
               }
           }
       } catch (IOException | RuntimeException ex) {
           redirectAttributes.addFlashAttribute("error", "No se pudo leer el archivo: " + ex.getMessage());
           return "redirect:/apartamentos";
       }

       redirectAttributes.addFlashAttribute("importSummary", "Importados: " + importados + ". Omitidos: " + omitidos + ".");
       if (!errores.isEmpty()) {
           redirectAttributes.addFlashAttribute("importErrors", errores);
       }
       return "redirect:/apartamentos";
   }

   private static String cell(DataFormatter formatter, Row row, int index) {
       return row == null || row.getCell(index) == null ? "" : formatter.formatCellValue(row.getCell(index)).trim();
   }

   

   @GetMapping("/apartamentos/editar/{id}")
   public String mostrarFormularioEditar(@PathVariable Long id, Model model) {

    Apartamento apartamento = apartamentoService.obtenerApartamentoPorId(id);

      model.addAttribute("apartamento", apartamento);
      model.addAttribute("titulo", "Editar Apartamento");
      model.addAttribute("currentPath", "/apartamentos");
      model.addAttribute("volverUrl", "/apartamentos");

      return "apartamentos/editar";

   }
 
   @PostMapping("/apartamentos/{id}")
   public String ActualizarApartamento(@PathVariable Long id,
                                       @ModelAttribute("apartamento") Apartamento apartamento) {
    Apartamento apartamentoExistente = apartamentoService.obtenerApartamentoPorId(id);
     
    apartamentoExistente.setNumero(apartamento.getNumero());
    apartamentoExistente.setTorre(apartamento.getTorre());
    apartamentoExistente.setPiso(apartamento.getPiso());
    apartamentoExistente.setEstado(apartamento.getEstado());


    apartamentoService.guardarApartamento(apartamentoExistente);
    

           return "redirect:/apartamentos";                           
         }
    
   
 
   
     @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartamentos/nuevo")
    public String mostrarFormularioCrear(Model model){
        Apartamento apartamento = new Apartamento();

        model.addAttribute("apartamento", apartamento);
        model.addAttribute("titulo", "Crear Apartamento");
        model.addAttribute("currentPath", "/apartamentos");
        model.addAttribute("volverUrl", "/apartamentos");

        return "apartamentos/crear";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/apartamentos")
    public String guardarApartamento(@ModelAttribute("apartamento") Apartamento apartamento) {
         
        apartamentoService.guardarApartamento(apartamento);

        return "redirect:/apartamentos";

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/apartamentos/eliminar/{id}")
     public String eliminarApartamento(@PathVariable Long id, RedirectAttributes redirectAttributes){
        try {
            apartamentoService.eliminarApartamento(id);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/apartamentos";
     }   

    
}
