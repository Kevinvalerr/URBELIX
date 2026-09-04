from datetime import datetime
from io import BytesIO
import sqlite3
from typing import Literal, Optional

from fastapi import FastAPI, HTTPException, status
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

app = FastAPI(title="URBELIX Reportes API", version="1.0.0")


class ReporteRegistro(BaseModel):
    tipo: Optional[str] = None
    entidad: Optional[str] = None
    residente: Optional[str] = None
    descripcion: Optional[str] = None
    fechaHora: Optional[datetime] = None


class IncidenciaEntrada(BaseModel):
    titulo: str = Field(min_length=1, max_length=150)
    descripcion: str = Field(min_length=1, max_length=4000)
    prioridad: Literal["BAJA", "MEDIA", "ALTA", "CRITICA"]
    estado: Literal["PENDIENTE", "EN_PROCESO", "RESUELTA", "RECHAZADA"] = "PENDIENTE"
    residenteId: Optional[int] = None
    apartamentoId: Optional[int] = None


class IncidenciaRespuesta(IncidenciaEntrada):
    id: int
    fechaCreacion: datetime
    fechaActualizacion: datetime


def conexion() -> sqlite3.Connection:
    db = sqlite3.connect("urbelix_fastapi.sqlite3")
    db.row_factory = sqlite3.Row
    db.execute("CREATE TABLE IF NOT EXISTS incidencias (id INTEGER PRIMARY KEY AUTOINCREMENT, titulo TEXT NOT NULL, descripcion TEXT NOT NULL, prioridad TEXT NOT NULL, estado TEXT NOT NULL, residente_id INTEGER, apartamento_id INTEGER, fecha_creacion TEXT NOT NULL, fecha_actualizacion TEXT NOT NULL)")
    return db


def respuesta(row: sqlite3.Row) -> IncidenciaRespuesta:
    return IncidenciaRespuesta(id=row["id"], titulo=row["titulo"], descripcion=row["descripcion"], prioridad=row["prioridad"], estado=row["estado"], residenteId=row["residente_id"], apartamentoId=row["apartamento_id"], fechaCreacion=datetime.fromisoformat(row["fecha_creacion"]), fechaActualizacion=datetime.fromisoformat(row["fecha_actualizacion"]))


def texto(value: Optional[str]) -> str:
    return value.strip() if value and value.strip() else "-"


@app.get("/health")
def health() -> dict[str, str]:
    db = conexion()
    db.close()
    return {"status": "ok", "service": "urbelix-reportes"}


@app.post("/incidencias", response_model=IncidenciaRespuesta, status_code=status.HTTP_201_CREATED)
def crear_incidencia(entrada: IncidenciaEntrada) -> IncidenciaRespuesta:
    if not entrada.titulo.strip() or not entrada.descripcion.strip():
        raise HTTPException(status_code=400, detail="Título y descripción son obligatorios")
    ahora = datetime.now().isoformat()
    db = conexion()
    cursor = db.execute("INSERT INTO incidencias (titulo, descripcion, prioridad, estado, residente_id, apartamento_id, fecha_creacion, fecha_actualizacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", (entrada.titulo.strip(), entrada.descripcion.strip(), entrada.prioridad, entrada.estado, entrada.residenteId, entrada.apartamentoId, ahora, ahora))
    db.commit()
    row = db.execute("SELECT * FROM incidencias WHERE id = ?", (cursor.lastrowid,)).fetchone()
    db.close()
    return respuesta(row)


@app.get("/incidencias", response_model=list[IncidenciaRespuesta])
def listar_incidencias(estado: Optional[str] = None, prioridad: Optional[str] = None) -> list[IncidenciaRespuesta]:
    db = conexion()
    rows = db.execute("SELECT * FROM incidencias WHERE (? IS NULL OR estado = ?) AND (? IS NULL OR prioridad = ?) ORDER BY fecha_creacion DESC", (estado, estado, prioridad, prioridad)).fetchall()
    db.close()
    return [respuesta(row) for row in rows]


@app.get("/incidencias/{incidencia_id}", response_model=IncidenciaRespuesta)
def obtener_incidencia(incidencia_id: int) -> IncidenciaRespuesta:
    db = conexion()
    row = db.execute("SELECT * FROM incidencias WHERE id = ?", (incidencia_id,)).fetchone()
    db.close()
    if row is None:
        raise HTTPException(status_code=404, detail="Incidencia no encontrada")
    return respuesta(row)


@app.put("/incidencias/{incidencia_id}", response_model=IncidenciaRespuesta)
def actualizar_incidencia(incidencia_id: int, entrada: IncidenciaEntrada) -> IncidenciaRespuesta:
    obtener_incidencia(incidencia_id)
    ahora = datetime.now().isoformat()
    db = conexion()
    db.execute("UPDATE incidencias SET titulo = ?, descripcion = ?, prioridad = ?, estado = ?, residente_id = ?, apartamento_id = ?, fecha_actualizacion = ? WHERE id = ?", (entrada.titulo.strip(), entrada.descripcion.strip(), entrada.prioridad, entrada.estado, entrada.residenteId, entrada.apartamentoId, ahora, incidencia_id))
    db.commit()
    row = db.execute("SELECT * FROM incidencias WHERE id = ?", (incidencia_id,)).fetchone()
    db.close()
    return respuesta(row)


@app.delete("/incidencias/{incidencia_id}", status_code=status.HTTP_204_NO_CONTENT)
def eliminar_incidencia(incidencia_id: int) -> None:
    obtener_incidencia(incidencia_id)
    db = conexion()
    db.execute("DELETE FROM incidencias WHERE id = ?", (incidencia_id,))
    db.commit()
    db.close()


@app.post("/incidencias/analizar")
def analizar_incidencia(entrada: IncidenciaEntrada) -> dict[str, object]:
    return {"status": "ok", "prioridad": entrada.prioridad, "estado": entrada.estado, "descripcionRecibida": bool(entrada.descripcion.strip())}


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
        "UrbelixTitle",
        parent=styles["Title"],
        fontName="Helvetica-Bold",
        fontSize=20,
        leading=24,
        textColor=colors.HexColor("#163B65"),
        alignment=TA_CENTER,
        spaceAfter=4,
    )
    subtitle_style = ParagraphStyle(
        "UrbelixSubtitle",
        parent=styles["Normal"],
        fontSize=10,
        textColor=colors.HexColor("#4B5563"),
        alignment=TA_CENTER,
        spaceAfter=12,
    )
    cell_style = ParagraphStyle(
        "UrbelixCell",
        parent=styles["Normal"],
        fontSize=8,
        leading=10,
        textColor=colors.HexColor("#1F2937"),
    )
    header_style = ParagraphStyle(
        "UrbelixHeader",
        parent=cell_style,
        fontName="Helvetica-Bold",
        textColor=colors.white,
        alignment=TA_CENTER,
    )

    story = [
        Paragraph("URBELIX", title_style),
        Paragraph(
            "REPORTE GENERAL DEL SISTEMA | Generado: "
            + datetime.now().strftime("%d/%m/%Y %H:%M")
            + " | Registros: "
            + str(len(registros)),
            subtitle_style,
        ),
    ]

    if registros:
        data = [[
            Paragraph("Tipo", header_style),
            Paragraph("Entidad", header_style),
            Paragraph("Residente", header_style),
            Paragraph("Descripción", header_style),
            Paragraph("Fecha", header_style),
        ]]
        for registro in registros:
            fecha = registro.fechaHora.strftime("%d/%m/%Y %H:%M") if registro.fechaHora else "-"
            data.append([
                Paragraph(texto(registro.tipo), cell_style),
                Paragraph(texto(registro.entidad), cell_style),
                Paragraph(texto(registro.residente), cell_style),
                Paragraph(texto(registro.descripcion), cell_style),
                Paragraph(fecha, cell_style),
            ])

        table = Table(data, colWidths=[27 * mm, 34 * mm, 42 * mm, 125 * mm, 34 * mm], repeatRows=1)
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#163B65")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#CBD5E1")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 6),
            ("RIGHTPADDING", (0, 0), (-1, -1), 6),
            ("TOPPADDING", (0, 0), (-1, -1), 6),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F1F5F9")]),
        ]))
        story.append(table)
    else:
        story.append(Spacer(1, 20))
        story.append(Paragraph("No hay registros para mostrar en el periodo actual.", cell_style))

    document.build(story)
    buffer.seek(0)
    return StreamingResponse(
        buffer,
        media_type="application/pdf",
        headers={"Content-Disposition": "attachment; filename=reporte_urbelix.pdf"},
    )
