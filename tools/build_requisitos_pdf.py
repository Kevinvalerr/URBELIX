from __future__ import annotations

import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    Flowable,
    KeepTogether,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "REQUISITOS_URBELIX.md"
OUTPUT_DIR = ROOT / "output" / "pdf"
OUTPUT = OUTPUT_DIR / "REQUISITOS_URBELIX.pdf"

NAVY = colors.HexColor("#104E64")
INK = colors.HexColor("#18232D")
MUTED = colors.HexColor("#5E6B75")
GRID = colors.HexColor("#B8C0C6")
MODULE_FILL = colors.HexColor("#A2A2A2")
SOFT_BLUE = colors.HexColor("#EAF3F6")
SOFT_GRAY = colors.HexColor("#F4F6F7")
GREEN = colors.HexColor("#16734A")
AMBER = colors.HexColor("#9A6500")
RED = colors.HexColor("#A12828")


def clean_inline(value: str) -> str:
    value = value.replace("`", "")
    value = value.replace("**", "")
    value = re.sub(r"\[([^\]]+)\]\([^\)]+\)", r"\1", value)
    return value.strip()


def module_name(section: str, nonfunctional: bool) -> str:
    if nonfunctional:
        return {
            "SEG": "Seguridad",
            "DAT": "Base de datos",
            "REN": "Rendimiento",
            "DIS": "Usabilidad",
            "ARQ": "Arquitectura",
            "OPS": "Operacion",
            "PRU": "Pruebas",
        }.get(section, section)
    return {
        "AUT": "Autenticacion",
        "ADM": "Administracion",
        "DAS": "Dashboard",
        "PAG": "Gestion financiera",
        "RES": "Reservas",
        "VIS": "Visitantes y porteria",
        "PAR": "Parqueaderos y vehiculos",
        "INC": "Incidencias y PQRS",
        "REP": "Reportes y comunicacion",
    }.get(section, section)


def priority_for(identifier: str, state: str) -> str:
    prefix = identifier.split("-")[1]
    number = int(identifier.split("-")[2])
    if identifier.startswith("RNF-"):
        if prefix in {"SEG", "DAT"} or identifier in {"RNF-OPS-03", "RNF-PRU-02"}:
            return "Alta"
        return "Media"
    if prefix in {"AUT", "DAS", "RES", "VIS"}:
        return "Alta"
    if prefix == "PAG":
        return "Alta" if number != 17 else "Media"
    if prefix == "ADM":
        return "Media" if number in {5, 9, 11, 13} else "Alta"
    if prefix == "PAR":
        return "Alta" if number in {5, 9, 10, 12} else "Media"
    if prefix == "INC":
        return "Alta" if number in {1, 2, 3, 4} else "Media"
    if prefix == "REP":
        return "Alta" if number in {3, 4, 5, 8} else "Media"
    return "Media" if state != "PENDIENTE" else "Alta"


def parse_requirements() -> tuple[list[dict], list[dict]]:
    functional: list[dict] = []
    nonfunctional: list[dict] = []
    current_section = ""
    current_nonfunctional = False
    for line in SOURCE.read_text(encoding="utf-8").splitlines():
        heading = re.match(r"^### 4\.\d+ (.+)$", line)
        if heading:
            current_section = heading.group(1)
            current_nonfunctional = False
            continue
        if line.startswith("## 5. Requisitos no funcionales"):
            current_nonfunctional = True
            current_section = ""
            continue
        if not line.startswith("| ") or line.startswith("| ---"):
            continue
        cells = [clean_inline(cell) for cell in line.strip().strip("|").split("|")]
        if not cells or not re.fullmatch(r"(?:RF|RNF)-[A-Z]+-\d+", cells[0]):
            continue
        identifier, requirement, state, evidence = cells[:4]
        section = identifier.split("-")[1]
        record = {
            "module": module_name(section, current_nonfunctional),
            "id": identifier,
            "requirement": requirement,
            "type": "No funcional" if current_nonfunctional else "Funcional",
            "priority": priority_for(identifier, state),
            "state": state,
            "evidence": evidence,
        }
        (nonfunctional if current_nonfunctional else functional).append(record)
    return functional, nonfunctional


class StatusKey(Flowable):
    def __init__(self, label: str, fill: colors.Color):
        super().__init__()
        self.label = label
        self.fill = fill
        self.width = 120
        self.height = 24

    def draw(self):
        self.canv.setFillColor(self.fill)
        self.canv.roundRect(0, 3, 9, 9, 2, stroke=0, fill=1)
        self.canv.setFillColor(INK)
        self.canv.setFont("Helvetica", 8.5)
        self.canv.drawString(15, 4, self.label)


def paragraph(text: str, style: ParagraphStyle) -> Paragraph:
    return Paragraph(text.replace("&", "&amp;"), style)


def make_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="CoverTitle", parent=styles["Title"], fontName="Helvetica-Bold",
        fontSize=25, leading=30, textColor=INK, alignment=TA_CENTER,
        spaceAfter=8,
    ))
    styles.add(ParagraphStyle(
        name="CoverSubtitle", parent=styles["Normal"], fontName="Helvetica",
        fontSize=12, leading=16, textColor=MUTED, alignment=TA_CENTER,
        spaceAfter=20,
    ))
    styles.add(ParagraphStyle(
        name="SectionTitle", parent=styles["Heading1"], fontName="Helvetica-Bold",
        fontSize=16, leading=20, textColor=NAVY, spaceBefore=12, spaceAfter=9,
    ))
    styles.add(ParagraphStyle(
        name="SubTitle", parent=styles["Heading2"], fontName="Helvetica-Bold",
        fontSize=12, leading=15, textColor=NAVY, spaceBefore=9, spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        name="Body", parent=styles["BodyText"], fontName="Helvetica",
        fontSize=9.5, leading=13, textColor=INK, spaceAfter=6,
    ))
    styles.add(ParagraphStyle(
        name="Small", parent=styles["BodyText"], fontName="Helvetica",
        fontSize=8, leading=10, textColor=MUTED, spaceAfter=3,
    ))
    styles.add(ParagraphStyle(
        name="TableHeader", parent=styles["BodyText"], fontName="Helvetica-Bold",
        fontSize=8.7, leading=10.5, textColor=colors.white, alignment=TA_LEFT,
    ))
    styles.add(ParagraphStyle(
        name="TableCell", parent=styles["BodyText"], fontName="Helvetica",
        fontSize=8.2, leading=10.4, textColor=INK,
    ))
    styles.add(ParagraphStyle(
        name="TableCellCenter", parent=styles["TableCell"], alignment=TA_CENTER,
    ))
    styles.add(ParagraphStyle(
        name="IdCell", parent=styles["TableCell"], fontSize=7.7, leading=9.2,
        alignment=TA_CENTER,
    ))
    styles.add(ParagraphStyle(
        name="ModuleCell", parent=styles["TableCell"], fontName="Helvetica-Bold",
        alignment=TA_LEFT,
    ))
    styles.add(ParagraphStyle(
        name="StatusCell", parent=styles["TableCell"], fontName="Helvetica-Bold",
        alignment=TA_CENTER,
    ))
    return styles


def build_requirement_table(records: list[dict], styles, include_status=False):
    widths = [1.12 * inch, 0.92 * inch, 2.98 * inch, 0.73 * inch, 0.75 * inch]
    if include_status:
        widths = [0.80 * inch, 0.94 * inch, 4.62 * inch, 0.94 * inch]
        header = ["ID", "Estado", "Evidencia / prueba", "Tipo"]
    else:
        header = ["Modulo", "ID", "Requisito", "Tipo", "Prioridad"]
    data = [[paragraph(item, styles["TableHeader"]) for item in header]]
    for row in records:
        if include_status:
            values = [row["id"], row["state"], row["evidence"], row["type"]]
            cells = [styles["IdCell"], styles["StatusCell"], styles["TableCell"], styles["TableCellCenter"]]
        else:
            values = [row["module"], row["id"], row["requirement"], row["type"], row["priority"]]
            cells = [styles["ModuleCell"], styles["IdCell"], styles["TableCell"], styles["TableCellCenter"], styles["TableCellCenter"]]
        data.append([paragraph(value, style) for value, style in zip(values, cells)])
    table = Table(data, colWidths=widths, splitByRow=0, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("GRID", (0, 0), (-1, -1), 0.45, GRID),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]
    if not include_status:
        commands.append(("BACKGROUND", (0, 1), (0, -1), MODULE_FILL))
    else:
        for index, row in enumerate(records, start=1):
            fill = {"IMPLEMENTADO": colors.HexColor("#E7F4EC"), "PARCIAL": colors.HexColor("#FFF4D8"), "PENDIENTE": colors.HexColor("#FCE8E8")}.get(row["state"], SOFT_GRAY)
            commands.append(("BACKGROUND", (1, index), (1, index), fill))
            commands.append(("TEXTCOLOR", (1, index), (1, index), {"IMPLEMENTADO": GREEN, "PARCIAL": AMBER, "PENDIENTE": RED}.get(row["state"], INK)))
        commands.append(("BACKGROUND", (0, 1), (-1, -1), colors.white))
    table.setStyle(TableStyle(commands))
    return table


def build_requirement_tables(records: list[dict], styles, include_status=False):
    """Return short tables so every printed block has a visible header."""
    chunk_size = 14 if not include_status else 12
    return [
        build_requirement_table(records[index:index + chunk_size], styles, include_status)
        for index in range(0, len(records), chunk_size)
    ]


def add_bullet(story, text: str, styles):
    story.append(Paragraph(f"-  {text}", styles["Body"]))


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(GRID)
    canvas.setLineWidth(0.45)
    canvas.line(doc.leftMargin, 0.58 * inch, letter[0] - doc.rightMargin, 0.58 * inch)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(doc.leftMargin, 0.37 * inch, "URBELIX | Especificacion de requisitos")
    canvas.drawRightString(letter[0] - doc.rightMargin, 0.37 * inch, f"Pagina {doc.page}")
    canvas.restoreState()


def build_pdf():
    functional, nonfunctional = parse_requirements()
    styles = make_styles()
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    document = SimpleDocTemplate(
        str(OUTPUT), pagesize=letter,
        leftMargin=0.55 * inch, rightMargin=0.55 * inch,
        topMargin=0.55 * inch, bottomMargin=0.78 * inch,
        title="Requisitos URBELIX", author="Equipo URBELIX",
    )
    story = []

    story.append(Spacer(1, 0.55 * inch))
    story.append(Paragraph("REQUISITOS DE URBELIX", styles["CoverTitle"]))
    story.append(Paragraph("Requisitos funcionales y no funcionales para pruebas", styles["CoverSubtitle"]))
    cover_meta = [
        [paragraph("Proyecto", styles["TableHeader"]), paragraph("URBELIX - sistema web de gestion residencial", styles["TableCell"])],
        [paragraph("Carpeta", styles["TableHeader"]), paragraph("URBELIXXX", styles["TableCell"])],
        [paragraph("Version", styles["TableHeader"]), paragraph("Rama develop | documento basado en el codigo actual", styles["TableCell"])],
        [paragraph("Fecha", styles["TableHeader"]), paragraph("03 de septiembre de 2026", styles["TableCell"])],
        [paragraph("Alcance", styles["TableHeader"]), paragraph("Aplicacion web de escritorio; pagos PSE/tarjeta en sandbox local", styles["TableCell"])],
    ]
    metadata = Table(cover_meta, colWidths=[1.15 * inch, 5.25 * inch], hAlign="CENTER")
    metadata.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, -1), NAVY),
        ("BACKGROUND", (1, 0), (1, -1), SOFT_BLUE),
        ("GRID", (0, 0), (-1, -1), 0.6, GRID),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 8),
        ("RIGHTPADDING", (0, 0), (-1, -1), 8),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
    ]))
    story.append(metadata)
    story.append(Spacer(1, 0.28 * inch))
    story.append(Paragraph("Documento fuente para planificar casos de prueba y validar el alcance real del proyecto.", styles["Body"]))
    status_table = Table([[StatusKey("Implementado", GREEN), StatusKey("Parcial", AMBER), StatusKey("Pendiente", RED)]], colWidths=[1.7 * inch] * 3, hAlign="CENTER")
    status_table.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "MIDDLE")]))
    story.append(status_table)
    story.append(PageBreak())

    story.append(Paragraph("1. Alcance y reglas de interpretacion", styles["SectionTitle"]))
    story.append(Paragraph(
        "Este documento consolida los requisitos que se pueden relacionar con la implementacion actual de URBELIXXX. "
        "Los estados no significan que el producto sea productivo: <b>Implementado</b> indica cobertura o verificacion local; "
        "<b>Parcial</b> indica que falta una validacion de ambiente o un caso; <b>Pendiente</b> identifica trabajo necesario antes de la version final.",
        styles["Body"],
    ))
    add_bullet(story, "La persistencia de desarrollo utiliza H2 en archivo; produccion esta preparada para MySQL y Flyway.", styles)
    add_bullet(story, "PSE y tarjeta se prueban como sandbox local, sin cobros reales ni dependencia del proveedor externo.", styles)
    add_bullet(story, "FastAPI/ReportLab es opcional para reportes; existe un generador PDF local de respaldo.", styles)
    add_bullet(story, "La prioridad es una propuesta basada en criticidad del flujo y no reemplaza la priorizacion del Product Owner.", styles)

    story.append(Paragraph("2. Actores y permisos resumidos", styles["SectionTitle"]))
    role_data = [
        [paragraph("Actor", styles["TableHeader"]), paragraph("Responsabilidad y limites", styles["TableHeader"])],
        [paragraph("ADMIN", styles["ModuleCell"]), paragraph("Administra usuarios, residentes, apartamentos, cartera, reservas, incidencias, avisos, reportes, auditoria y catalogo de parqueaderos. No opera entradas y salidas de porteria.", styles["TableCell"])],
        [paragraph("RESIDENTE", styles["ModuleCell"]), paragraph("Gestiona sus pagos, reservas, visitantes, vehiculos, incidencias y perfil. No puede ver informacion de terceros ni aprobar accesos.", styles["TableCell"])],
        [paragraph("PORTERIA", styles["ModuleCell"]), paragraph("Opera solicitudes de visitantes, entradas, salidas y movimientos de parqueadero. No tiene apartamento, cartera, reservas ni funciones administrativas.", styles["TableCell"])],
    ]
    roles = Table(role_data, colWidths=[1.25 * inch, 5.15 * inch], hAlign="LEFT")
    roles.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY), ("BACKGROUND", (0, 1), (0, -1), MODULE_FILL),
        ("GRID", (0, 0), (-1, -1), 0.45, GRID), ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6), ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story.append(roles)
    story.append(PageBreak())

    story.append(Paragraph("3. Requisitos funcionales", styles["SectionTitle"]))
    story.append(Paragraph(f"Total: {len(functional)} requisitos funcionales. El formato sigue el modelo de referencia.", styles["Small"]))
    functional_tables = build_requirement_tables(functional, styles)
    for table in functional_tables:
        story.append(KeepTogether([table, Spacer(1, 8)]))

    nonfunctional_tables = build_requirement_tables(nonfunctional, styles)
    story.append(KeepTogether([
        Paragraph("4. Requisitos no funcionales", styles["SectionTitle"]),
        Paragraph(f"Total: {len(nonfunctional)} requisitos no funcionales.", styles["Small"]),
        nonfunctional_tables[0],
        Spacer(1, 8),
    ]))
    for table in nonfunctional_tables[1:]:
        story.append(KeepTogether([table, Spacer(1, 8)]))

    all_records = functional + nonfunctional
    verification_tables = build_requirement_tables(all_records, styles, include_status=True)
    story.append(KeepTogether([
        Paragraph("5. Matriz de verificacion para pruebas", styles["SectionTitle"]),
        Paragraph(
            "La siguiente matriz conserva la evidencia asociada a cada requisito. Las filas pendientes son el backlog minimo para poder declarar una version final limpia.",
            styles["Body"],
        ),
        verification_tables[0],
        Spacer(1, 8),
    ]))
    for table in verification_tables[1:]:
        story.append(KeepTogether([table, Spacer(1, 8)]))

    story.append(PageBreak())
    story.append(Paragraph("6. Flujos de aceptacion prioritarios", styles["SectionTitle"]))
    flows = [
        ("FA-01 Registro y primer ingreso", [
            "Crear un apartamento con codigo de registro.",
            "Registrar una persona con datos validos y codigo correcto.",
            "Comprobar que se crea RESIDENTE asociado al apartamento.",
            "Comprobar cambio obligatorio de contrasena en el primer ingreso.",
            "Rechazar codigo incorrecto, correo o documento duplicado y apartamento inexistente.",
        ]),
        ("FA-02 Separacion estricta de roles", [
            "Verificar que ADMIN accede a administracion y reportes.",
            "Verificar que RESIDENTE solo ve y opera sus registros.",
            "Verificar que PORTERIA solo opera visitantes y parqueaderos.",
            "Intentar rutas prohibidas y comprobar respuesta 403 o acceso denegado.",
            "Comprobar que PORTERIA no tiene residente, apartamento, cartera ni reservas.",
        ]),
        ("FA-03 Pago simulado y factura", [
            "ADMIN crea un pago PSE o tarjeta pendiente.",
            "RESIDENTE inicia el checkout y obtiene referencia unica.",
            "Simular aprobado, pendiente, rechazado, anulado y error.",
            "Comprobar que solo aprobado marca PAGADO y registra fecha.",
            "Descargar factura y comprobar que el residente no ve pagos ajenos.",
        ]),
        ("FA-04 Visitante y porteria", [
            "RESIDENTE crea una solicitud asociada a su apartamento.",
            "PORTERIA aprueba o rechaza con motivo opcional.",
            "Registrar entrada solo si el visitante fue aprobado.",
            "Registrar salida solo si el visitante esta dentro.",
            "Comprobar el ciclo PENDIENTE, APROBADA, DENTRO y FINALIZADA.",
        ]),
        ("FA-05 Incidencia con trazabilidad", [
            "RESIDENTE crea una incidencia y consulta solo sus registros.",
            "ADMIN cambia estado, responde y puede cerrar el caso.",
            "Agregar comentarios y evidencias validas.",
            "Comprobar notificaciones internas y correo si SMTP esta habilitado.",
            "Comprobar que una incidencia ajena no es accesible.",
        ]),
    ]
    for title, steps in flows:
        story.append(Paragraph(title, styles["SubTitle"]))
        for index, step in enumerate(steps, start=1):
            story.append(Paragraph(f"{index}. {step}", styles["Body"]))

    story.append(Paragraph("7. Pendientes que bloquean la version final", styles["SectionTitle"]))
    pending = [record for record in all_records if record["state"] == "PENDIENTE"]
    for record in pending:
        story.append(Paragraph(f"<b>{record['id']}</b> - {record['requirement']}", styles["Body"]))
    story.append(Spacer(1, 6))
    story.append(Paragraph(
        "Criterio de salida: ejecutar FA-01 a FA-05 en navegador, validar MySQL/Flyway y SMTP en aceptacion, configurar respaldo/restauracion y medir rendimiento antes de declarar release productivo.",
        styles["Body"],
    ))

    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print(f"Created {OUTPUT}")
    print(f"Functional requirements: {len(functional)}")
    print(f"Non-functional requirements: {len(nonfunctional)}")
    print(f"Pending: {len(pending)}")


if __name__ == "__main__":
    build_pdf()
