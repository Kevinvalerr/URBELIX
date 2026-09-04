from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "diagrams" / "CU-01_iniciar_sesion.png"
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


def font(size, bold=False):
    path = BOLD if bold else REGULAR
    return ImageFont.truetype(str(path), size)


def hex_color(value):
    return value if value.startswith("#") else f"#{value}"


def arrow(draw, start, end, color=GOLD, width=4):
    draw.line((start[0], start[1], end[0], end[1]), fill=hex_color(color), width=width)
    import math

    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    length = 18
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
    draw.text((x - 20, y + 153), label, font=font(20, True), fill=hex_color(NAVY))


def oval(draw, box, label, fill=LIGHT_TEAL, outline=DEEP_BLUE, size=21):
    draw.ellipse(box, fill=hex_color(fill), outline=hex_color(outline), width=3)
    left, top, right, bottom = box
    bounds = draw.textbbox((0, 0), label, font=font(size, True))
    tw = bounds[2] - bounds[0]
    th = bounds[3] - bounds[1]
    draw.text(((left + right - tw) / 2, (top + bottom - th) / 2 - 2), label, font=font(size, True), fill=hex_color(INK))


def line_label(draw, x, y, label, color=MUTED):
    draw.text((x, y), label, font=font(17, True), fill=hex_color(color))


def main():
    image = Image.new("RGB", (1600, 1000), "white")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 1600, 120), fill="#F4F7F9")
    draw.rectangle((0, 0, 18, 1000), fill=f"#{NAVY}")
    draw.text((55, 28), "CU-01: Iniciar sesión", font=font(36, True), fill=f"#{NAVY}")
    draw.text((55, 78), "Caso de uso UML para autenticación y acceso por rol", font=font(21), fill=f"#{MUTED}")

    draw.rounded_rectangle((350, 160, 1510, 820), radius=24, fill="#FCFDFE", outline=hex_color(NAVY), width=4)
    draw.text((395, 188), "Sistema URBELIX", font=font(26, True), fill=f"#{DEEP_BLUE}")
    actor(draw, 125, 420, "Usuario")

    oval(draw, (590, 385, 1030, 485), "Autenticar usuario", fill=LIGHT_TEAL)
    oval(draw, (430, 250, 810, 330), "Validar credenciales", fill=LIGHT_BLUE, size=19)
    oval(draw, (1010, 250, 1390, 330), "Determinar rol", fill=LIGHT_BLUE, size=19)
    oval(draw, (1010, 530, 1390, 610), "Redirigir dashboard", fill=LIGHT_BLUE, size=19)
    oval(draw, (430, 650, 810, 730), "Forzar cambio inicial", fill=LIGHT_GOLD, size=19)
    oval(draw, (1010, 650, 1390, 730), "Denegar acceso", fill=LIGHT_RED, outline="#A12828", size=19)

    arrow(draw, (175, 490), (590, 435), color=GOLD)
    arrow(draw, (700, 385), (620, 330), color=GOLD, width=3)
    arrow(draw, (920, 385), (1110, 330), color=GOLD, width=3)
    arrow(draw, (920, 485), (1110, 530), color=GOLD, width=3)
    arrow(draw, (620, 650), (680, 485), color=GOLD, width=3)
    arrow(draw, (1110, 650), (930, 485), color=GOLD, width=3)
    line_label(draw, 495, 328, "<<include>>")
    line_label(draw, 955, 328, "<<include>>")
    line_label(draw, 965, 505, "<<include>>")
    line_label(draw, 690, 555, "<<extend>>", color="#9A6500")
    line_label(draw, 945, 555, "<<extend>>", color="#A12828")

    draw.rounded_rectangle((70, 800, 1510, 930), radius=16, fill="#F4F6F7", outline=hex_color(GRID), width=2)
    draw.text((100, 825), "Precondición: el usuario está en la página pública y posee una cuenta activa.", font=font(19, True), fill=hex_color(INK))
    draw.text((100, 865), "Resultado: sesión autenticada y navegación limitada a ADMIN, RESIDENTE o PORTERIA.", font=font(19), fill=hex_color(INK))
    draw.text((100, 905), "Nota: primer ingreso y credenciales inválidas son extensiones del flujo principal.", font=font(18), fill=f"#{MUTED}")

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    main()
