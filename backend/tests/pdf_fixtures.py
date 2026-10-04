"""Generates small in-memory text-based PDF fixtures for pipeline/upload tests."""
from io import BytesIO

from reportlab.pdfgen import canvas


def make_text_pdf() -> bytes:
    buf = BytesIO()
    c = canvas.Canvas(buf, pagesize=(612, 792))

    c.drawString(72, 720, "Unit 1: Introduction to Testing")
    c.drawString(72, 700, "This section covers the basics of software testing.")
    c.drawString(72, 680, "Testing verifies that code behaves as expected under many conditions.")
    c.showPage()

    c.drawString(72, 720, "Unit 2: Advanced Testing")
    c.drawString(72, 700, "This section covers mocking, fixtures, and coverage analysis.")
    c.drawString(72, 680, "Good tests are fast, isolated, repeatable, and self-checking.")
    c.showPage()

    c.save()
    return buf.getvalue()


def make_empty_pdf() -> bytes:
    """A structurally valid PDF with no extractable text (simulates a scanned doc)."""
    buf = BytesIO()
    c = canvas.Canvas(buf, pagesize=(612, 792))
    c.showPage()
    c.save()
    return buf.getvalue()
