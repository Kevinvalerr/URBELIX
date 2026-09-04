from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "output" / "diagrams" / "casos_uso"
FONT_DIR = Path(r"C:\Windows\Fonts")
REGULAR = FONT_DIR / "segoeui.ttf"
BOLD = FONT_DIR / "segoeuib.ttf"

NAVY = "1F4E78"
DEEP_BLUE = "0F4C5C"
GOLD = "C7952D"
INK = "1F2933"
MUTED = "5E6B75"
GRID = "C7D1D8"
LIGHT_BLUE = "EAF2F8"
LIGHT_TEAL = "E9F4F3"
LIGHT_GOLD = "FFF7E5"
LIGHT_RED = "FDECEC"


def fnt(size, bold=False):
    return ImageFont.truetype(str(BOLD if bold else REGULAR), size)


def hex_color(value):
    return value if value.startswith("#") else f"#{value}"


def centered(draw, box, text, size=19, bold=True, fill=INK):
    lines = text.split("\n")
    heights = []
    widths = []
    for line in lines:
        bounds = draw.textbbox((0, 0), line, font=fnt(size, bold))
        widths.append(bounds[2] - bounds[0])
        heights.append(bounds[3] - bounds[1])
    total_height = sum(heights) + max(0, len(lines) - 1) * 4
    y = (box[1] + box[3] - total_height) / 2
    for line, width, height in zip(lines, widths, heights):
        draw.text(((box[0] + box[2] - width) / 2, y), line, font=fnt(size, bold), fill=hex_color(fill))
        y += height + 4


def oval(draw, box, text, fill=LIGHT_TEAL, outline=DEEP_BLUE, size=19):
    draw.ellipse(box, fill=hex_color(fill), outline=hex_color(outline), width=3)
    centered(draw, box, text, size=size)


def arrow(draw, start, end, color=GOLD, width=3):
    draw.line((start[0], start[1], end[0], end[1]), fill=hex_color(color), width=width)
    import math

    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    length = 17
    left = (end[0] - length * math.cos(angle - 0.45), end[1] - length * math.sin(angle - 0.45))
    right = (end[0] - length * math.cos(angle + 0.45), end[1] - length * math.sin(angle + 0.45))
    draw.polygon([end, left, right], fill=hex_color(color))


def actor(draw, x, y, label):
    draw.ellipse((x, y, x + 44, y + 44), fill=hex_color(LIGHT_BLUE), outline=hex_color(NAVY), width=3)
    draw.line((x + 22, y + 44, x + 22, y + 105), fill=hex_color(NAVY), width=3)
    draw.line((x + 22, y + 60, x - 6, y + 84), fill=hex_color(NAVY), width=3)
    draw.line((x + 22, y + 60, x + 50, y + 84), fill=hex_color(NAVY), width=3)
    draw.line((x + 22, y + 105, x - 4, y + 140), fill=hex_color(NAVY), width=3)
    draw.line((x + 22, y + 105, x + 48, y + 140), fill=hex_color(NAVY), width=3)
    size = 15 if len(label) > 18 else 18
    centered(draw, (20, y + 148, 280, y + 205), label, size=size, fill=NAVY)


CASES = [
    ("CU-01", "Iniciar sesión", "ADMIN / RESIDENTE / PORTERIA", "Validar credenciales\ny determinar rol", ["Validar credenciales", "Determinar rol", "Redirigir dashboard"], ["Forzar cambio inicial", "Denegar acceso"], "Cuenta activa y usuario en página pública.", "Sesión creada y acceso limitado por rol."),
    ("CU-02", "Cerrar sesión", "Usuario autenticado", "Cerrar sesión", ["Invalidar sesión"], [], "Existe una sesión activa.", "Sesión y cookie invalidadas."),
    ("CU-03", "Recuperar contraseña", "Usuario no autenticado", "Solicitar recuperación", ["Generar token", "Enviar correo"], ["Token vencido"], "El usuario conoce su correo.", "Enlace temporal enviado sin revelar si el correo existe."),
    ("CU-04", "Cambiar contraseña", "Usuario autenticado / enlace válido", "Cambiar contraseña", ["Validar complejidad", "Guardar con BCrypt"], ["Token usado"], "Existe sesión o token válido.", "Contraseña actualizada y token invalidado."),
    ("CU-05", "Registrar residente", "Visitante", "Crear cuenta residente", ["Validar código de apartamento", "Validar duplicados"], ["Rechazar datos inválidos"], "Existe apartamento y código de registro.", "Cuenta RESIDENTE creada con cambio inicial pendiente."),
    ("CU-06", "Actualizar perfil", "ADMIN / RESIDENTE", "Actualizar datos propios", ["Validar datos"], ["Rechazar modificación ajena"], "Usuario autenticado.", "Nombre o teléfono actualizado."),
    ("CU-07", "Gestionar usuarios y roles", "ADMIN", "Administrar cuenta", ["Validar rol y relaciones", "Registrar auditoría"], ["Impedir cero admins"], "ADMIN autenticado.", "Cuenta creada, editada, activada o desactivada."),
    ("CU-08", "Gestionar residentes", "ADMIN", "Administrar residente", ["Validar documento", "Relacionar apartamento"], ["Importar desde Excel"], "ADMIN autenticado.", "Residente consistente y trazable."),
    ("CU-09", "Gestionar apartamentos", "ADMIN", "Administrar apartamento", ["Validar código", "Proteger relaciones"], ["Impedir eliminación con datos"], "ADMIN autenticado.", "Apartamento creado, editado o eliminado de forma segura."),
    ("CU-10", "Registrar y administrar pagos", "ADMIN", "Registrar pago", ["Validar residente-apartamento", "Registrar auditoría"], ["Rechazar estado inválido"], "ADMIN autenticado.", "Obligación guardada con monto, fechas y estado."),
    ("CU-11", "Generar cuota mensual", "ADMIN", "Generar cuotas", ["Consultar residentes activos", "Evitar duplicados"], [], "Existe un periodo válido.", "Cuotas faltantes creadas una sola vez."),
    ("CU-12", "Pagar mediante sandbox", "RESIDENTE", "Simular pago", ["Generar referencia", "Validar monto y evento"], ["Simular rechazo o error"], "Existe pago propio pendiente o vencido.", "Estado actualizado sin cobro real."),
    ("CU-13", "Consultar y descargar factura", "RESIDENTE / ADMIN", "Descargar factura", ["Validar propiedad o permiso", "Generar PDF"], [], "Existe pago visible para el actor.", "Factura PDF descargada con trazabilidad."),
    ("CU-14", "Generar reportes y Excel", "ADMIN", "Generar reporte", ["Aplicar filtros", "Registrar generación"], ["Exportar PDF o Excel"], "ADMIN autenticado.", "Reporte generado sin exponer datos no autorizados."),
    ("CU-15", "Gestionar reservas", "RESIDENTE / ADMIN", "Gestionar reserva", ["Validar fechas", "Verificar cruces"], ["Aprobar o rechazar"], "Usuario autenticado y zona disponible.", "Reserva guardada con estado válido."),
    ("CU-16", "Solicitar visitante", "RESIDENTE", "Crear solicitud", ["Validar documento", "Asociar apartamento"], [], "Residente con apartamento.", "Solicitud creada en estado PENDIENTE."),
    ("CU-17", "Aprobar o rechazar visitante", "PORTERIA", "Resolver solicitud", ["Validar estado pendiente", "Notificar residente"], ["Registrar motivo de rechazo"], "Existe solicitud pendiente.", "Solicitud APROBADA o RECHAZADA."),
    ("CU-18", "Registrar entrada y salida", "PORTERIA", "Registrar movimiento visitante", ["Validar autorización", "Actualizar estado"], ["Rechazar transición inválida"], "Visitante aprobado o dentro.", "Entrada o salida registrada."),
    ("CU-19", "Gestionar parqueaderos", "ADMIN", "Administrar espacio", ["Validar tipo y estado", "Proteger historial"], ["Impedir eliminación relacionada"], "ADMIN autenticado.", "Catálogo de parqueaderos consistente."),
    ("CU-20", "Gestionar vehículos propios", "RESIDENTE", "Administrar vehículo", ["Validar placa", "Relacionar residente"], ["Rechazar vehículo ajeno"], "Residente autenticado.", "Vehículo propio creado, editado o desactivado."),
    ("CU-21", "Registrar movimiento de parqueadero", "PORTERIA", "Registrar entrada o salida", ["Validar placa y espacio", "Conservar historial"], ["Rechazar ingreso duplicado"], "Existe vehículo y espacio compatible.", "Movimiento guardado y espacio actualizado."),
    ("CU-22", "Crear incidencia o PQRS", "RESIDENTE", "Crear incidencia", ["Validar asunto y descripción", "Proteger adjuntos"], ["Agregar evidencia"], "Residente autenticado.", "Incidencia ABIERTA asociada al residente."),
    ("CU-23", "Gestionar incidencia", "ADMIN", "Atender incidencia", ["Cambiar estado", "Registrar observación"], ["Rechazar con motivo"], "Existe incidencia visible para ADMIN.", "Incidencia resuelta o cerrada con historial."),
    ("CU-24", "Consultar avisos y notificaciones", "ADMIN / RESIDENTE / PORTERIA", "Consultar comunicación", ["Filtrar por vigencia y rol"], ["Marcar como leída"], "Usuario autenticado.", "Información visible según alcance."),
    ("CU-25", "Consultar auditoría", "ADMIN", "Consultar bitácora", ["Filtrar por actor, acción o fecha"], [], "ADMIN autenticado.", "Mutaciones consultables sin secretos ni contraseñas."),
]


def make_case(case):
    code, title, actors, main, includes, extends, pre, post = case
    image = Image.new("RGB", (1600, 1000), "white")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 1600, 120), fill="#F4F7F9")
    draw.rectangle((0, 0, 18, 1000), fill=hex_color(NAVY))
    draw.text((55, 28), f"{code}: {title}", font=fnt(35, True), fill=hex_color(NAVY))
    draw.text((55, 78), "Diagrama UML de caso de uso", font=fnt(21), fill=hex_color(MUTED))
    draw.rounded_rectangle((350, 150, 1530, 800), radius=24, fill="#FCFDFE", outline=hex_color(NAVY), width=4)
    draw.text((395, 180), "Sistema URBELIX", font=fnt(26, True), fill=hex_color(DEEP_BLUE))
    actor(draw, 125, 410, actors)

    oval(draw, (600, 385, 1050, 490), main, fill=LIGHT_TEAL, size=20)
    positions = [(390, 220), (780, 220), (1170, 220), (390, 575), (780, 575), (1170, 575)]
    for index, label in enumerate(includes):
        x, y = positions[index]
        oval(draw, (x, y, x + 330, y + 82), label, fill=LIGHT_BLUE, size=17)
        if y < 400:
            arrow(draw, (760 + (index % 2) * 20, 385), (x + 165, y + 82), width=3)
        else:
            arrow(draw, (760 + (index % 2) * 20, 490), (x + 165, y), width=3)
        draw.text((x + 18, y + 90 if y < 400 else y - 30), "<<include>>", font=fnt(16, True), fill=hex_color(MUTED))
    for index, label in enumerate(extends):
        x, y = positions[len(includes) + index]
        oval(draw, (x, y, x + 330, y + 82), label, fill=LIGHT_GOLD, size=17)
        if y < 400:
            arrow(draw, (x + 165, y + 82), (760, 385), width=3)
        else:
            arrow(draw, (x + 165, y), (760, 490), width=3)
        draw.text((x + 18, y + 90 if y < 400 else y - 30), "<<extend>>", font=fnt(16, True), fill=hex_color(GOLD))
    arrow(draw, (175, 480), (600, 438), width=4)

    draw.rounded_rectangle((70, 825, 1530, 950), radius=16, fill="#F4F6F7", outline=hex_color(GRID), width=2)
    draw.text((100, 850), f"Precondición: {pre}", font=fnt(18, True), fill=hex_color(INK))
    draw.text((100, 888), f"Resultado: {post}", font=fnt(18), fill=hex_color(INK))
    draw.text((100, 922), "Las relaciones muestran dependencias UML; los pasos detallados se describen en actividad y secuencia.", font=fnt(17), fill=hex_color(MUTED))
    image.save(OUTPUT_DIR / f"{code}_{title.lower().replace(' ', '_').replace('/', '_')}.png")


def main():
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    for case in CASES:
        make_case(case)
    print(f"Created {len(CASES)} use-case images in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
