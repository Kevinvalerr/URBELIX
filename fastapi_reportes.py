from datetime import datetime
from io import BytesIO
from typing import Optional
from xml.sax.saxutils import escape

from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

app = FastAPI(title="URBELIX Reportes API", version="1.0.0")


class ReporteRegistro(BaseModel):
    tipo: Optional[str] = None
    entidad: Optional[str] = None
    residente: Optional[str] = None
    descripcion: Optional[str] = None
    fechaHora: Optional[datetime] = None


def texto(value: Optional[str]) -> str:
    return escape(value.strip()) if value and value.strip() else "-"


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "service": "urbelix-reportes"}


@app.post("/reportes/generar-pdf")
def generar_pdf(registros: list[ReporteRegistro]) -> StreamingResponse:
    buffer = BytesIO()
    document = SimpleDocTemplate(
        buffer,
        pagesize=landscape(A4),
        rightMargin=14 * mm,
        leftMargin=14 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="Reporte general URBELIX",
        author="URBELIX",
    )

    styles = getSampleStyleSheet()
    title_style = ParagraphStyle(
        "UrbelixTitle", parent=styles["Title"], fontName="Helvetica-Bold",
        fontSize=20, leading=24, textColor=colors.HexColor("#163B65"),
        alignment=TA_CENTER, spaceAfter=4,
    )
    subtitle_style = ParagraphStyle(
        "UrbelixSubtitle", parent=styles["Normal"], fontSize=10,
        textColor=colors.HexColor("#4B5563"), alignment=TA_CENTER, spaceAfter=12,
    )
    cell_style = ParagraphStyle(
        "UrbelixCell", parent=styles["Normal"], fontSize=8, leading=10,
        textColor=colors.HexColor("#1F2937"),
    )
    header_style = ParagraphStyle(
        "UrbelixHeader", parent=cell_style, fontName="Helvetica-Bold",
        textColor=colors.white, alignment=TA_CENTER,
    )

    story = [
        Paragraph("URBELIX", title_style),
        Paragraph(
            "REPORTE GENERAL DEL SISTEMA | Generado: "
            + datetime.now().strftime("%d/%m/%Y %H:%M")
            + " | Registros: " + str(len(registros)),
            subtitle_style,
        ),
    ]

    if registros:
        data = [[
            Paragraph("Tipo", header_style),
            Paragraph("Entidad", header_style),
            Paragraph("Residente", header_style),
            Paragraph("Descripcion", header_style),
            Paragraph("Fecha", header_style),
        ]]
        for registro in registros:
            fecha = registro.fechaHora.strftime("%d/%m/%Y %H:%M") if registro.fechaHora else "-"
            data.append([
                Paragraph(texto(registro.tipo), cell_style),
                Paragraph(texto(registro.entidad), cell_style),
                Paragraph(texto(registro.residente), cell_style),
                Paragraph(texto(registro.descripcion), cell_style),
                Paragraph(escape(fecha), cell_style),
            ])

        table = Table(data, colWidths=[25 * mm, 35 * mm, 45 * mm, 125 * mm, 35 * mm], repeatRows=1)
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#2A9D8F")),
            ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F4F7FB")]),
            ("LINEBELOW", (0, 0), (-1, 0), 1.2, colors.HexColor("#163B65")),
            ("LEFTPADDING", (0, 0), (-1, -1), 5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 5),
            ("TOPPADDING", (0, 0), (-1, -1), 5),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ]))
        story.append(table)
    else:
        story.append(Paragraph("No hay registros para los filtros seleccionados.", cell_style))

    story.append(Spacer(1, 10))
    document.build(story)
    buffer.seek(0)
    return StreamingResponse(
        buffer,
        media_type="application/pdf",
        headers={"Content-Disposition": "attachment; filename=reporte_urbelix.pdf"},
    )
