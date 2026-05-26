"""Generate branded AiCMM Product Brochure PDF from Markdown."""
import sys
import re
from fpdf import FPDF


class BrochurePDF(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font('Arial', '', r'C:\Windows\Fonts\arial.ttf')
        self.add_font('Arial', 'B', r'C:\Windows\Fonts\arialbd.ttf')
        self.add_font('Arial', 'I', r'C:\Windows\Fonts\ariali.ttf')
        self.add_font('Consolas', '', r'C:\Windows\Fonts\consola.ttf')
        self.set_auto_page_break(auto=True, margin=20)
        self.set_margins(15, 15, 15)

    def header(self):
        if self.page_no() > 1:
            self.set_font('Arial', 'I', 8)
            self.set_text_color(128, 128, 128)
            self.cell(0, 5, 'AiCMM - Agent Capability Maturity Model | Product Brochure', align='C')
            self.ln(8)
            self.set_text_color(0, 0, 0)

    def footer(self):
        self.set_y(-15)
        self.set_font('Arial', 'I', 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f'Page {self.page_no()}', align='C')

    def chapter_title(self, text, level=1):
        sizes = {1: 22, 2: 16, 3: 13, 4: 11}
        colors = {1: (25, 25, 112), 2: (0, 100, 0), 3: (70, 70, 70), 4: (90, 90, 90)}
        self.set_font('Arial', 'B', sizes.get(level, 11))
        r, g, b = colors.get(level, (0, 0, 0))
        self.set_text_color(r, g, b)
        if level == 1:
            self.ln(6)
        text = sanitize(text)
        try:
            self.multi_cell(0, sizes.get(level, 11) * 0.5, text)
        except Exception:
            pass
        self.set_text_color(0, 0, 0)
        if level <= 2:
            self.ln(2)

    def body_text(self, text):
        self.set_font('Arial', size=10)
        # Remove problematic Unicode chars that may be too wide
        text = sanitize(text)
        if len(text) > 200:
            text = text[:197] + '...'
        try:
            self.multi_cell(0, 5, text)
        except Exception:
            # Fallback: just skip the problematic line
            pass
        self.ln(1)

    def code_text(self, text):
        self.set_font('Consolas', size=8)
        self.set_fill_color(245, 245, 245)
        lines = text.split('\n')
        for line in lines:
            line = sanitize(line)
            if len(line) > 95:
                line = line[:92] + '...'
            try:
                self.cell(0, 4, line, new_x='LMARGIN', new_y='NEXT', fill=True)
            except Exception:
                pass
        self.set_font('Arial', size=10)
        self.ln(2)

    def italic_text(self, text):
        self.set_font('Arial', 'I', 10)
        text = sanitize(text)
        try:
            self.multi_cell(0, 5, text)
        except Exception:
            pass
        self.set_font('Arial', size=10)


def clean(text):
    """Strip Markdown inline formatting."""
    text = re.sub(r'\*\*(.*?)\*\*', r'\1', text)
    text = re.sub(r'\*(.*?)\*', r'\1', text)
    text = re.sub(r'\[(.*?)\]\(.*?\)', r'\1', text)
    text = re.sub(r'`(.*?)`', r'\1', text)
    return text


def sanitize(text):
    """Remove emoji and problematic Unicode characters."""
    # Remove emoji ranges
    text = re.sub(r'[\U0001F300-\U0001F9FF]', '', text)
    text = re.sub(r'[\U00002600-\U000027BF]', '', text)
    text = re.sub(r'[\U0000FE00-\U0000FEFF]', '', text)
    # Replace common Unicode with ASCII
    text = text.replace('\u2192', '->')  # arrow
    text = text.replace('\u2190', '<-')
    text = text.replace('\u2022', '-')   # bullet
    text = text.replace('\u2714', '[x]')  # checkmark
    text = text.replace('\u2718', '[ ]')  # X mark
    text = text.replace('\u2502', '|')
    text = text.replace('\u2500', '-')
    text = text.replace('\u250c', '+')
    text = text.replace('\u2510', '+')
    text = text.replace('\u2514', '+')
    text = text.replace('\u2518', '+')
    text = text.replace('\u251c', '+')
    text = text.replace('\u2524', '+')
    text = text.replace('\u253c', '+')
    text = text.replace('\u2550', '=')
    text = text.replace('\u2551', '|')
    text = text.replace('\u2588', '#')
    text = text.replace('\u2591', '.')
    text = text.replace('\u2593', '#')
    text = text.replace('\u2580', '#')
    text = text.replace('\u2584', '#')
    text = text.replace('\u2503', '|')
    text = text.replace('\u2501', '-')
    text = text.replace('\u250f', '+')
    text = text.replace('\u2513', '+')
    text = text.replace('\u2517', '+')
    text = text.replace('\u251b', '+')
    text = text.replace('\u2523', '+')
    text = text.replace('\u252b', '+')
    text = text.replace('\u254b', '+')
    text = text.replace('\u2265', '>=')
    text = text.replace('\u2264', '<=')
    text = text.replace('\u2260', '!=')
    text = text.replace('\u00a9', '(c)')
    # Remove any remaining chars outside basic multilingual plane
    text = ''.join(c for c in text if ord(c) < 0xFFFF)
    return text


def generate_brochure():
    md_path = r'C:\Users\sures\Projects\copilot\Arima-AiCMM\docs\PRODUCT-BROCHURE.md'
    pdf_path = r'C:\Users\sures\Projects\copilot\Arima-AiCMM\docs\AiCMM-Product-Brochure.pdf'

    with open(md_path, 'r', encoding='utf-8') as f:
        content = f.read()

    pdf = BrochurePDF()

    # --- Title Page ---
    pdf.add_page()
    pdf.ln(35)
    pdf.set_font('Arial', 'B', 32)
    pdf.set_text_color(25, 25, 112)
    pdf.cell(0, 15, 'AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.set_font('Arial', 'B', 18)
    pdf.cell(0, 10, 'Agent Capability Maturity Model', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(8)
    pdf.set_font('Arial', 'I', 13)
    pdf.set_text_color(70, 70, 70)
    pdf.cell(0, 8, 'The Universal Framework for Evaluating,', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 8, 'Classifying, and Governing AI Agents', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(20)

    # Decorative line
    pdf.set_draw_color(25, 25, 112)
    pdf.set_line_width(0.8)
    pdf.line(40, pdf.get_y(), 170, pdf.get_y())
    pdf.ln(10)

    pdf.set_font('Arial', size=11)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(0, 7, '12 Dimensions | 7 Governance Rules | Level 0 + Level 1 Scoring', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 7, 'Pure Java | MCP Server | REST API | CLI Agents', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(15)

    pdf.set_font('Arial', size=10)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(0, 6, 'Version 0.2.0 | Open Source | Apache 2.0 License', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 6, 'https://github.com/snchande/Arima-AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(25)

    pdf.set_draw_color(25, 25, 112)
    pdf.set_line_width(0.3)
    pdf.line(60, pdf.get_y(), 150, pdf.get_y())
    pdf.ln(8)
    pdf.set_text_color(0, 0, 0)
    pdf.set_font('Arial', 'I', 10)
    pdf.cell(0, 6, 'Created by Suresh Chande', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 6, 'github.com/snchande | linkedin.com/in/sureshchande', align='C', new_x='LMARGIN', new_y='NEXT')

    # --- Process content ---
    in_code = False
    code_buf = []
    lines = content.split('\n')
    skip_header = True

    for line in lines:
        line = line.rstrip()

        # Skip badge/title header until first ## section
        if skip_header:
            if line.startswith('## ') and 'Table of Contents' not in line:
                skip_header = False
            elif line.startswith('## ') and 'Table of Contents' in line:
                # Skip TOC section
                continue
            else:
                continue

        # Code block handling
        if line.strip().startswith('```'):
            if in_code:
                in_code = False
                if code_buf:
                    pdf.code_text('\n'.join(code_buf))
                code_buf = []
            else:
                in_code = True
            continue

        if in_code:
            code_buf.append(line)
            continue

        if not line.strip():
            pdf.ln(2)
            continue

        # HTML divs - skip
        if line.strip().startswith('<') and not line.strip().startswith('<a'):
            continue

        # Headings
        if line.startswith('####'):
            pdf.chapter_title(clean(line.lstrip('#').strip()), 4)
        elif line.startswith('###'):
            pdf.chapter_title(clean(line.lstrip('#').strip()), 3)
        elif line.startswith('##'):
            pdf.add_page()
            pdf.chapter_title(clean(line.lstrip('#').strip()), 2)
        elif line.startswith('#'):
            pdf.chapter_title(clean(line.lstrip('#').strip()), 1)
        elif line.strip() in ('---', '***', '___'):
            pdf.ln(2)
            pdf.set_draw_color(200, 200, 200)
            pdf.line(pdf.get_x(), pdf.get_y(), pdf.get_x() + 180, pdf.get_y())
            pdf.ln(3)
        elif line.strip().startswith('- ') or line.strip().startswith('* '):
            indent = (len(line) - len(line.lstrip())) // 2
            text = clean(line.strip()[2:])
            prefix = '  ' * indent + '- '
            pdf.body_text(prefix + text)
        elif re.match(r'^\d+\.', line.strip()):
            text = re.sub(r'^\d+\.\s*', '', line.strip())
            num = re.match(r'^(\d+\.)', line.strip()).group(1)
            pdf.body_text('  ' + num + ' ' + clean(text))
        elif line.strip().startswith('>'):
            text = line.strip().lstrip('>').strip()
            pdf.italic_text('    ' + clean(text))
        elif '|' in line:
            cells = [c.strip() for c in line.split('|') if c.strip()]
            if cells and not all(set(c) <= set('-: ') for c in cells):
                row = '  |  '.join(clean(c) for c in cells[:5])
                if len(row) > 105:
                    row = row[:102] + '...'
                pdf.body_text(row)
        else:
            text = clean(line)
            if text.strip():
                pdf.body_text(text)

    pdf.output(pdf_path)
    print(f'Created: {pdf_path}')
    print(f'Pages: {pdf.page_no()}')
    print(f'Size: {round(len(open(pdf_path, "rb").read()) / 1024)}KB')


if __name__ == '__main__':
    generate_brochure()
