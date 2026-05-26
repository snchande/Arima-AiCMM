"""Generate branded AiCMM Product Brochure PDF from Markdown.
Renders Mermaid diagrams as images using mmdc (mermaid-cli) and embeds them.
Tables rendered with proper formatting. Executive-friendly layout."""
import os
import re
import subprocess
import tempfile
from fpdf import FPDF


DOCS_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(DOCS_DIR)
MD_PATH = os.path.join(DOCS_DIR, 'PRODUCT-BROCHURE.md')
PDF_PATH = os.path.join(DOCS_DIR, 'AiCMM-Product-Brochure.pdf')


def sanitize(text):
    """Remove emoji and problematic Unicode characters."""
    text = re.sub(r'[\U0001F300-\U0001F9FF]', '', text)
    text = re.sub(r'[\U00002600-\U000027BF]', '', text)
    text = re.sub(r'[\U0000FE00-\U0000FEFF]', '', text)
    text = text.replace('\u2192', '->').replace('\u2190', '<-')
    text = text.replace('\u2022', '-').replace('\u2714', '[x]').replace('\u2718', '[ ]')
    text = text.replace('\u2265', '>=').replace('\u2264', '<=').replace('\u00a9', '(c)')
    text = text.replace('\u2502', '|').replace('\u2500', '-')
    text = text.replace('\u250c', '+').replace('\u2510', '+')
    text = text.replace('\u2514', '+').replace('\u2518', '+')
    text = text.replace('\u251c', '+').replace('\u2524', '+').replace('\u253c', '+')
    text = text.replace('\u2588', '#').replace('\u2591', '.').replace('\u2584', '#')
    text = ''.join(c for c in text if ord(c) < 0xFFFF)
    return text


def clean(text):
    """Strip Markdown inline formatting."""
    text = re.sub(r'\*\*(.*?)\*\*', r'\1', text)
    text = re.sub(r'\*(.*?)\*', r'\1', text)
    text = re.sub(r'\[(.*?)\]\(.*?\)', r'\1', text)
    text = re.sub(r'`(.*?)`', r'\1', text)
    return text


def render_mermaid_to_png(mermaid_code, output_path):
    """Use mmdc (mermaid-cli) to render mermaid code to PNG."""
    mmdc_cmd = os.path.join(os.environ.get('APPDATA', ''), 'npm', 'mmdc.cmd')
    if not os.path.exists(mmdc_cmd):
        mmdc_cmd = 'mmdc'  # fallback to PATH

    with tempfile.NamedTemporaryFile(mode='w', suffix='.mmd', delete=False, encoding='utf-8') as f:
        f.write(mermaid_code)
        mmd_path = f.name

    try:
        result = subprocess.run(
            [mmdc_cmd, '-i', mmd_path, '-o', output_path,
             '--width', '900', '--backgroundColor', 'white',
             '--theme', 'neutral', '--quiet'],
            capture_output=True, text=True, timeout=30, shell=True
        )
        if result.returncode != 0:
            print(f'  Warning: mmdc failed: {result.stderr[:100]}')
            return False
        return os.path.exists(output_path)
    except Exception as e:
        print(f'  Warning: mmdc error: {e}')
        return False
    finally:
        os.unlink(mmd_path)


class BrochurePDF(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font('Arial', '', r'C:\Windows\Fonts\arial.ttf')
        self.add_font('Arial', 'B', r'C:\Windows\Fonts\arialbd.ttf')
        self.add_font('Arial', 'I', r'C:\Windows\Fonts\ariali.ttf')
        self.add_font('Consolas', '', r'C:\Windows\Fonts\consola.ttf')
        self.set_auto_page_break(auto=True, margin=20)
        self.set_margins(18, 18, 18)

    def header(self):
        if self.page_no() > 1:
            self.set_font('Arial', 'I', 7)
            self.set_text_color(140, 140, 140)
            self.cell(90, 4, 'AiCMM | Agent Capability Maturity Model')
            self.cell(0, 4, f'Page {self.page_no()}', align='R')
            self.ln(6)
            self.set_draw_color(220, 220, 220)
            self.set_line_width(0.2)
            self.line(18, self.get_y(), 192, self.get_y())
            self.ln(4)
            self.set_text_color(0, 0, 0)

    def footer(self):
        self.set_y(-12)
        self.set_font('Arial', 'I', 7)
        self.set_text_color(160, 160, 160)
        self.cell(0, 5, 'github.com/snchande/Arima-AiCMM | Apache 2.0 | (c) 2026 Suresh Chande', align='C')

    def section_title(self, text, level=2):
        sizes = {1: 20, 2: 14, 3: 11, 4: 10}
        if level <= 2:
            self.ln(4)
        self.set_font('Arial', 'B', sizes.get(level, 10))
        if level == 1:
            self.set_text_color(25, 25, 112)
        elif level == 2:
            self.set_text_color(0, 100, 80)
        else:
            self.set_text_color(50, 50, 50)
        text = sanitize(text)
        try:
            self.multi_cell(0, sizes.get(level, 10) * 0.55, text)
        except Exception:
            pass
        self.set_text_color(0, 0, 0)
        self.ln(2 if level <= 2 else 1)

    def para(self, text):
        self.set_font('Arial', size=9)
        text = sanitize(text)
        if len(text) > 250:
            text = text[:247] + '...'
        try:
            self.multi_cell(0, 4.5, text)
        except Exception:
            pass
        self.ln(1)

    def bullet(self, text, indent=0):
        self.set_font('Arial', size=9)
        text = sanitize(text)
        prefix = '    ' * indent + '  -  '
        try:
            self.multi_cell(0, 4.5, prefix + text)
        except Exception:
            pass
        self.ln(0.5)

    def code_block(self, lines):
        self.set_font('Consolas', size=7)
        self.set_fill_color(248, 248, 248)
        self.set_draw_color(200, 200, 200)
        x = self.get_x()
        self.set_line_width(0.2)
        self.line(x, self.get_y(), x + 174, self.get_y())
        self.ln(1)
        for line in lines[:20]:
            line = sanitize(line)
            if len(line) > 92:
                line = line[:89] + '...'
            try:
                self.cell(174, 3.5, '  ' + line, new_x='LMARGIN', new_y='NEXT', fill=True)
            except Exception:
                pass
        if len(lines) > 20:
            self.cell(174, 3.5, '  ... (truncated)', new_x='LMARGIN', new_y='NEXT', fill=True)
        self.line(x, self.get_y(), x + 174, self.get_y())
        self.set_font('Arial', size=9)
        self.ln(3)

    def embed_diagram(self, png_path):
        """Embed a rendered diagram PNG into the PDF."""
        if not os.path.exists(png_path):
            return
        # Check if we need a page break (estimate image height ~60mm)
        if self.get_y() + 65 > 270:
            self.add_page()
        try:
            self.image(png_path, x=20, w=170)
            self.ln(4)
        except Exception as e:
            print(f'  Warning: Could not embed image: {e}')

    def render_table(self, headers, rows):
        """Render a proper formatted table with colored header and alternating rows."""
        if not headers:
            return

        page_width = 174
        num_cols = len(headers)

        col_widths = []
        for i in range(num_cols):
            max_len = len(sanitize(clean(headers[i])))
            for row in rows[:10]:
                if i < len(row):
                    max_len = max(max_len, len(sanitize(clean(row[i]))))
            col_widths.append(max(3, max_len))

        total = sum(col_widths) or 1
        col_widths = [max(12, (w / total) * page_width) for w in col_widths]
        scale = page_width / sum(col_widths)
        col_widths = [w * scale for w in col_widths]

        needed = (len(rows) + 1) * 5.5 + 8
        if self.get_y() + min(needed, 60) > 270:
            self.add_page()

        # Header - navy background
        self.set_font('Arial', 'B', 8)
        self.set_fill_color(25, 25, 112)
        self.set_text_color(255, 255, 255)
        self.set_draw_color(25, 25, 112)
        for i, h in enumerate(headers):
            text = sanitize(clean(h)).strip()
            max_chars = int(col_widths[i] / 2)
            if len(text) > max_chars:
                text = text[:max_chars - 2] + '..'
            self.cell(col_widths[i], 6, ' ' + text, border=1, fill=True)
        self.ln()

        # Data rows
        self.set_font('Arial', size=8)
        self.set_text_color(30, 30, 30)
        self.set_draw_color(200, 200, 200)
        for row_idx, row in enumerate(rows):
            if row_idx % 2 == 0:
                self.set_fill_color(248, 250, 252)
            else:
                self.set_fill_color(255, 255, 255)
            for i in range(num_cols):
                text = sanitize(clean(row[i])).strip() if i < len(row) else ''
                max_chars = int(col_widths[i] / 2)
                if len(text) > max_chars:
                    text = text[:max_chars - 2] + '..'
                try:
                    self.cell(col_widths[i], 5.5, ' ' + text, border=1, fill=True)
                except Exception:
                    self.cell(col_widths[i], 5.5, ' ?', border=1, fill=True)
            self.ln()

        self.set_text_color(0, 0, 0)
        self.ln(3)


def generate_brochure():
    with open(MD_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pre-render all mermaid diagrams to PNG
    print('Rendering Mermaid diagrams...')
    mermaid_blocks = re.findall(r'```mermaid\n(.*?)```', content, re.DOTALL)
    diagram_pngs = []
    temp_dir = tempfile.mkdtemp(prefix='aicmm_diagrams_')

    for i, block in enumerate(mermaid_blocks):
        png_path = os.path.join(temp_dir, f'diagram_{i}.png')
        print(f'  Rendering diagram {i + 1}/{len(mermaid_blocks)}...')
        success = render_mermaid_to_png(block, png_path)
        diagram_pngs.append(png_path if success else None)

    print(f'  {sum(1 for p in diagram_pngs if p)} of {len(mermaid_blocks)} diagrams rendered.')

    # Build PDF
    pdf = BrochurePDF()

    # === TITLE PAGE ===
    pdf.add_page()
    pdf.ln(40)
    pdf.set_font('Arial', 'B', 36)
    pdf.set_text_color(25, 25, 112)
    pdf.cell(0, 18, 'AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.set_font('Arial', 'B', 16)
    pdf.cell(0, 10, 'Agent Capability Maturity Model', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(6)
    pdf.set_font('Arial', 'I', 12)
    pdf.set_text_color(70, 70, 70)
    pdf.cell(0, 7, 'The Universal Framework for Evaluating,', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 7, 'Classifying, and Governing AI Agents', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(15)

    pdf.set_draw_color(25, 25, 112)
    pdf.set_line_width(0.6)
    pdf.line(50, pdf.get_y(), 160, pdf.get_y())
    pdf.ln(10)

    pdf.set_font('Arial', 'B', 10)
    pdf.set_text_color(0, 100, 80)
    for h in [
        '12 Universal Dimensions  |  Scored 0-5',
        '7 Governance Rules  |  Automated Validation',
        'Level 0 Universal + Level 1 Domain-Specific',
        'Pure Java  |  MCP Server  |  REST API  |  CLI Agents',
        'Open Source  |  Apache 2.0  |  Extensible',
    ]:
        pdf.cell(0, 7, h, align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(20)

    pdf.set_draw_color(25, 25, 112)
    pdf.set_line_width(0.3)
    pdf.line(65, pdf.get_y(), 145, pdf.get_y())
    pdf.ln(8)
    pdf.set_font('Arial', size=10)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(0, 6, 'Version 0.2.0', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 6, 'https://github.com/snchande/Arima-AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(8)
    pdf.set_font('Arial', 'I', 9)
    pdf.cell(0, 5, 'Created by Suresh Chande', align='C', new_x='LMARGIN', new_y='NEXT')

    # === PROCESS CONTENT ===
    in_code = False
    code_buf = []
    in_mermaid = False
    mermaid_idx = 0
    in_table = False
    table_headers = []
    table_rows = []
    skip_header = True
    skip_toc = False

    lines = content.split('\n')

    for line in lines:
        line = line.rstrip()

        if skip_header:
            if line.startswith('## ') and 'Table of Contents' in line:
                skip_toc = True
                skip_header = False
                continue
            elif line.startswith('## '):
                skip_header = False
            else:
                continue

        if skip_toc:
            if line.startswith('## ') and 'Table of Contents' not in line:
                skip_toc = False
            else:
                continue

        # Flush table if leaving table context
        if in_table and not ('|' in line and not line.strip().startswith('```')):
            pdf.render_table(table_headers, table_rows)
            in_table = False
            table_headers = []
            table_rows = []

        # Code/mermaid block handling
        if line.strip().startswith('```'):
            if in_code:
                in_code = False
                if code_buf:
                    pdf.code_block(code_buf)
                code_buf = []
            elif in_mermaid:
                in_mermaid = False
                # Embed the pre-rendered diagram
                if mermaid_idx <= len(diagram_pngs) and diagram_pngs[mermaid_idx - 1]:
                    pdf.embed_diagram(diagram_pngs[mermaid_idx - 1])
                else:
                    pdf.para('[Diagram could not be rendered]')
            else:
                if 'mermaid' in line:
                    in_mermaid = True
                    mermaid_idx += 1
                else:
                    in_code = True
            continue

        if in_code:
            code_buf.append(line)
            continue
        if in_mermaid:
            continue  # Skip mermaid content lines (already pre-rendered)

        if not line.strip():
            if not in_table:
                pdf.ln(1.5)
            continue

        if line.strip().startswith('<') and not line.strip().startswith('<a'):
            continue

        # Table handling
        if '|' in line and not line.strip().startswith('#'):
            cells = [c.strip() for c in line.split('|')]
            if cells and cells[0] == '':
                cells = cells[1:]
            if cells and cells[-1] == '':
                cells = cells[:-1]
            if all(set(c) <= set('-: ') for c in cells):
                continue
            if not in_table:
                in_table = True
                table_headers = cells
            else:
                table_rows.append(cells)
            continue

        # Headings
        if line.startswith('####'):
            pdf.section_title(clean(line.lstrip('#').strip()), 4)
        elif line.startswith('###'):
            pdf.section_title(clean(line.lstrip('#').strip()), 3)
        elif line.startswith('##'):
            pdf.add_page()
            pdf.section_title(clean(line.lstrip('#').strip()), 2)
        elif line.startswith('#'):
            pdf.section_title(clean(line.lstrip('#').strip()), 1)
        elif line.strip() in ('---', '***', '___'):
            pdf.ln(2)
            pdf.set_draw_color(220, 220, 220)
            pdf.line(18, pdf.get_y(), 192, pdf.get_y())
            pdf.ln(3)
        elif line.strip().startswith('- ') or line.strip().startswith('* '):
            indent = (len(line) - len(line.lstrip())) // 4
            text = clean(line.strip()[2:])
            pdf.bullet(text, indent)
        elif re.match(r'^\s*\d+\.', line):
            text = re.sub(r'^\s*\d+\.\s*', '', line)
            num = re.match(r'^\s*(\d+\.)', line).group(1)
            pdf.para('  ' + num + ' ' + clean(text))
        elif line.strip().startswith('>'):
            text = line.strip().lstrip('>').strip()
            pdf.set_font('Arial', 'I', 9)
            pdf.set_text_color(80, 80, 80)
            try:
                pdf.multi_cell(0, 4.5, '    ' + sanitize(clean(text)))
            except Exception:
                pass
            pdf.set_text_color(0, 0, 0)
            pdf.set_font('Arial', size=9)
        else:
            text = clean(line)
            if text.strip():
                pdf.para(text)

    # Flush remaining table
    if in_table:
        pdf.render_table(table_headers, table_rows)

    pdf.output(PDF_PATH)

    # Cleanup temp files
    for p in diagram_pngs:
        if p and os.path.exists(p):
            os.unlink(p)
    try:
        os.rmdir(temp_dir)
    except OSError:
        pass

    size_kb = round(os.path.getsize(PDF_PATH) / 1024)
    print(f'\nCreated: {PDF_PATH}')
    print(f'Pages: {pdf.page_no()}')
    print(f'Size: {size_kb}KB')


if __name__ == '__main__':
    generate_brochure()
