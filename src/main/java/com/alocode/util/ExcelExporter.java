package com.alocode.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.alocode.service.ReporteService;
import com.alocode.model.Pedido;
import com.alocode.model.DetallePedido;
import java.text.SimpleDateFormat;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class ExcelExporter {
    private static final String FONT_NAME = "Arial";
    private static final short HEADER_FONT_SIZE = 11;
    private static final short BODY_FONT_SIZE = 10;
    private static final short TITLE_FONT_SIZE = 14;
    
    public static void exportToExcel(Object reporte, String nombreArchivo, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Estilos predefinidos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle centeredStyle = createCenteredDataStyle(workbook);
            CellStyle centeredCurrencyStyle = createCenteredCurrencyStyle(workbook);
            CellStyle separatorStyle = createSeparatorStyle(workbook);
            
            if (reporte instanceof ReporteService.ReporteDiario diario) {
                Sheet sheet = workbook.createSheet("Reporte Diario");
                
                // Configurar anchos de columnas (en unidades de 1/256 de ancho de carácter)
                sheet.setColumnWidth(0, 12*256);  // Columna A
                sheet.setColumnWidth(1, 20*256);  // Columna B
                sheet.setColumnWidth(2, 20*256);  // Columna C
                sheet.setColumnWidth(3, 15*256);  // Columna D
                sheet.setColumnWidth(4, 15*256);  // Columna E
                sheet.setColumnWidth(5, 25*256);  // Columna F
                sheet.setColumnWidth(6, 20*256);  // Columna G
                
                int rowIdx = 0;
                
                // Título del reporte
                Row titleRow = sheet.createRow(rowIdx++);
                titleRow.setHeightInPoints(25);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("REPORTE DIARIO");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
                
                // Información general
                rowIdx = addGeneralInfo(sheet, rowIdx, 
                    new String[] {"Fecha", "Monto Apertura", "Monto Cierre", "Total Ventas", "Total Recargos", "Total Neto"},
                    new Object[] {
                        diario.getFecha(),
                        diario.getMontoApertura(),
                        diario.getMontoCierre(),
                        diario.getTotalVentas(),
                        diario.getTotalRecargos(),
                        diario.getTotalNeto()
                    },
                    headerStyle, centeredStyle, centeredCurrencyStyle, dateStyle);
                
                // Espacio antes de la tabla de pedidos
                rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);
                
                // Cabecera de pedidos
                String[] pedidosHeaders = {
                    "ID Pedido", "Mesa", "Usuario", "Total", "Recargo", "Observaciones", "Fecha Pagado"
                };
                rowIdx = createTableHeader(sheet, rowIdx, pedidosHeaders, headerStyle);
                
                // Datos de pedidos
                boolean firstPedido = true;
                for (Pedido p : diario.getPedidos()) {
                    if (!firstPedido) {
                        // Agregar separador entre pedidos
                        rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);
                    }
                    firstPedido = false;
                    
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20);
                    
                    int colIdx = 0;
                    
                    addCell(row, colIdx++, p.getId(), centeredStyle);
                    addCell(row, colIdx++, p.getMesa() != null ? String.valueOf(p.getMesa().getNumero()) : "", centeredStyle);
                    addCell(row, colIdx++, p.getUsuario() != null ? p.getUsuario().getNombre() : "", centeredStyle);
                    addCell(row, colIdx++, p.getTotal(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getRecargo(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getObservaciones() != null ? p.getObservaciones() : "", centeredStyle);
                    addCell(row, colIdx++, p.getFechaPagado() != null ? p.getFechaPagado() : null, dateStyle);
                    
                    // Detalles del pedido
                    if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                        rowIdx++;  // Espacio antes de los detalles
                        
                        // Título de detalles
                        Row detTitleRow = sheet.createRow(rowIdx++);
                        detTitleRow.setHeightInPoints(20);
                        Cell detTitleCell = detTitleRow.createCell(1);
                        detTitleCell.setCellValue("DETALLES DEL PEDIDO");
                        detTitleCell.setCellStyle(headerStyle);
                        sheet.addMergedRegion(new CellRangeAddress(detTitleRow.getRowNum(), detTitleRow.getRowNum(), 1, 4));
                        
                        // Cabecera de detalles
                        String[] detallesHeaders = {"Producto", "Cantidad", "Precio Unitario", "Subtotal"};
                        rowIdx = createTableHeader(sheet, rowIdx, detallesHeaders, headerStyle, 1);
                        
                        // Datos de detalles
                        for (DetallePedido d : p.getDetalles()) {
                            Row detRow = sheet.createRow(rowIdx++);
                            detRow.setHeightInPoints(18);
                            colIdx = 1;
                            
                            addCell(detRow, colIdx++, d.getProducto() != null ? d.getProducto().getNombre() : "", centeredStyle);
                            addCell(detRow, colIdx++, d.getCantidad(), centeredStyle);
                            addCell(detRow, colIdx++, d.getPrecioUnitario(), centeredCurrencyStyle);
                            addCell(detRow, colIdx++, d.getSubtotal(), centeredCurrencyStyle);
                        }
                    }
                }
                
            } else if (reporte instanceof ReporteService.ReporteSemanal semanal) {
                // Implementación similar para reporte semanal...
            } else if (reporte instanceof ReporteService.ReporteMensual mensual) {
                // Implementación similar para reporte mensual...
            }
            
            // Escribir el archivo
            String fechaHora = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String nombreFinal = nombreArchivo + "_" + fechaHora + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + nombreFinal);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Métodos auxiliares para estilos
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(HEADER_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(BODY_FONT_SIZE);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createCenteredDataStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
    
    private static CellStyle createCenteredCurrencyStyle(Workbook workbook) {
        CellStyle style = createCurrencyStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createCenteredDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
        return style;
    }
    
    private static CellStyle createSeparatorStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    // Métodos auxiliares para manipulación de celdas
    
    private static int addGeneralInfo(Sheet sheet, int rowIdx, String[] labels, Object[] values, 
            CellStyle labelStyle, CellStyle valueStyle, CellStyle currencyStyle, CellStyle dateStyle) {
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(20);
            
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(labelStyle);
            
            Cell valueCell = row.createCell(1);
            if (values[i] instanceof Number) {
                valueCell.setCellValue(((Number) values[i]).doubleValue());
                valueCell.setCellStyle(currencyStyle);
            } else if (values[i] instanceof Date) {
                valueCell.setCellValue((Date) values[i]);
                valueCell.setCellStyle(dateStyle);
            } else {
                valueCell.setCellValue(values[i].toString());
                valueCell.setCellStyle(valueStyle);
            }
            
            // Combinar celdas para el valor
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 6));
        }
        return rowIdx;
    }
    
    private static int createTableHeader(Sheet sheet, int rowIdx, String[] headers, CellStyle style) {
        return createTableHeader(sheet, rowIdx, headers, style, 0);
    }
    
    private static int createTableHeader(Sheet sheet, int rowIdx, String[] headers, CellStyle style, int startCol) {
        Row headerRow = sheet.createRow(rowIdx++);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(startCol + i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        return rowIdx;
    }
    
    private static int addSeparatorRow(Sheet sheet, int rowIdx, int colsToMerge, CellStyle style) {
        Row separatorRow = sheet.createRow(rowIdx++);
        separatorRow.setHeightInPoints(6); // Altura pequeña para el separador
        
        Cell separatorCell = separatorRow.createCell(0);
        separatorCell.setCellValue("");
        separatorCell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(separatorRow.getRowNum(), separatorRow.getRowNum(), 0, colsToMerge - 1));
        
        return rowIdx;
    }
    
    private static void addCell(Row row, int colIdx, Object value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }
}