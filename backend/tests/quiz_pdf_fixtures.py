"""A richer PDF fixture than tests/pdf_fixtures.py's make_text_pdf, with more
distinct vocabulary per section so MockAIProvider has enough material to build
several distinct-option questions per topic in quiz-generation tests."""
from io import BytesIO

from reportlab.pdfgen import canvas


def make_quiz_source_pdf() -> bytes:
    buf = BytesIO()
    c = canvas.Canvas(buf, pagesize=(612, 792))

    lines_unit1 = [
        "Unit 1: Photosynthesis Fundamentals",
        "Photosynthesis converts sunlight into chemical energy inside chloroplasts.",
        "Chlorophyll absorbs light primarily in the blue and red wavelengths.",
        "The process produces glucose molecules and releases oxygen as a byproduct.",
        "Stomata regulate gas exchange between the leaf and surrounding atmosphere.",
        "Water molecules are split during the light-dependent reactions of photosynthesis.",
    ]
    y = 720
    for line in lines_unit1:
        c.drawString(72, y, line)
        y -= 20
    c.showPage()

    lines_unit2 = [
        "Unit 2: Cellular Respiration Basics",
        "Mitochondria generate adenosine triphosphate through cellular respiration pathways.",
        "Glycolysis breaks glucose molecules into smaller pyruvate compounds efficiently.",
        "The citric acid cycle releases carbon dioxide during oxidative metabolism.",
        "Electron transport chains produce most cellular energy during respiration.",
        "Anaerobic respiration produces lactic acid when oxygen availability becomes limited.",
    ]
    y = 720
    for line in lines_unit2:
        c.drawString(72, y, line)
        y -= 20
    c.showPage()

    c.save()
    return buf.getvalue()
