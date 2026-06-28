"""
AiCMM Framework Whitepaper PDF Generator
Generates a polished, professional whitepaper explaining the AiCMM framework.
Includes radar chart examples, dimension scoring guide, and Agent Card customization.
"""
import math
import os
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
from fpdf import FPDF

DOCS_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(DOCS_DIR)
PDF_PATH = os.path.join(DOCS_DIR, 'AiCMM-Framework-Whitepaper.pdf')
PPTX_DIR = os.path.join(DOCS_DIR, 'images', 'pptx')
CARDS_DIR = os.path.join(DOCS_DIR, 'images', 'cards')

# --- Colors ---
NAVY = (25, 25, 112)
TEAL = (0, 100, 80)
DARK_GRAY = (50, 50, 50)
LIGHT_GRAY = (140, 140, 140)
ACCENT_BLUE = (37, 99, 235)
ACCENT_GREEN = (5, 150, 105)
BG_LIGHT = (248, 250, 252)

# --- Dimension Data (FULL NAMES - never abbreviated) ---
DIMENSIONS = [
    {"pos": 0, "key": "autonomy", "name": "Autonomy", "group": "Cognitive Core",
     "definition": "The degree to which an agent can act independently, make decisions, and execute tasks without human intervention. Measures self-direction from fully human-controlled to fully self-governing.",
     "question": "How self-directed is it?"},
    {"pos": 1, "key": "reasoning", "name": "Reasoning & Planning", "group": "Cognitive Core",
     "definition": "The ability to solve problems under uncertainty, decompose complex tasks into steps, plan sequences of actions, and adapt strategy when conditions change. Includes logical inference, causal reasoning, and multi-step planning.",
     "question": "Can it solve problems under uncertainty?"},
    {"pos": 2, "key": "memory", "name": "Memory & Context", "group": "Cognitive Core",
     "definition": "The capacity to retain, retrieve, and utilize information over time. Includes short-term working memory, long-term persistent storage, episodic recall, and temporal awareness of past interactions.",
     "question": "Does it retain and use information over time?"},
    {"pos": 3, "key": "learning", "name": "Learning & Adaptation", "group": "Cognitive Core",
     "definition": "The ability to improve performance from experience safely. Includes pattern recognition from feedback, behavioral adjustment, knowledge acquisition, and the capacity to generalize from specific examples without catastrophic forgetting.",
     "question": "Does it improve from experience safely?"},
    {"pos": 4, "key": "toolUse", "name": "Tool Use & Integration", "group": "Action & Integration",
     "definition": "Proficiency in discovering, selecting, orchestrating, and error-handling external tools, APIs, databases, and services. Measures the sophistication of tool selection logic, parallel execution, retry strategies, and graceful degradation.",
     "question": "Can it orchestrate tools and handle failures?"},
    {"pos": 5, "key": "collaboration", "name": "Collaboration & Social Intelligence", "group": "Action & Integration",
     "definition": "The capacity to coordinate effectively with humans and other AI agents. Includes turn-taking, delegation, empathy modeling, conflict resolution, inclusivity awareness, and the ability to adapt communication style to the audience.",
     "question": "Does it coordinate with humans and other agents?"},
    {"pos": 6, "key": "embodiment", "name": "Embodiment", "group": "Action & Integration",
     "definition": "Physical or virtual presence involving perception, navigation, manipulation, and spatial awareness. Score 0 for pure software agents. Measures the sophistication of sensorimotor integration for agents with physical bodies or sensor arrays.",
     "question": "Does it interact with the physical world?"},
    {"pos": 7, "key": "explainability", "name": "Explainability & Transparency", "group": "Trust & Deployment",
     "definition": "The ability to justify its actions, expose decision-making rationale, provide audit trails, and support human review. Includes proactive disclosure of uncertainty, confidence levels, and reasoning chains that humans can verify.",
     "question": "Can it explain decisions transparently?"},
    {"pos": 8, "key": "safety", "name": "Safety & Robustness", "group": "Trust & Deployment",
     "definition": "The capacity to operate within safe boundaries, fail gracefully under adversarial or unexpected conditions, resist prompt injection, maintain containment, and degrade service rather than cause harm. Includes red-team resilience.",
     "question": "Does it operate safely with guardrails?"},
    {"pos": 9, "key": "interoperability", "name": "Interoperability & Standards", "group": "Trust & Deployment",
     "definition": "Adherence to open standards (A2A, MCP, OpenAI API) enabling the agent to participate in multi-agent ecosystems, exchange structured data, and be discovered and composed by other systems without custom integration work.",
     "question": "Does it follow standards (A2A, MCP)?"},
    {"pos": 10, "key": "costEfficiency", "name": "Cost & Resource Efficiency", "group": "Trust & Deployment",
     "definition": "Awareness of computational, financial, and energy costs. Includes token optimization, caching strategies, request batching, ability to choose cheaper models for simpler tasks, and staying within budget constraints at scale.",
     "question": "Is it resource-efficient and cost-aware?"},
    {"pos": 11, "key": "domainAlignment", "name": "Domain Alignment & Governance", "group": "Trust & Deployment",
     "definition": "Compliance with industry regulations, organizational policies, ethical guidelines, and audit requirements specific to the deployment domain. The final gate before production: is it safe, legal, and appropriate to deploy?",
     "question": "Is it compliant, auditable, deployable?"},
]

# Full radar chart labels (meaningful short forms, NOT abbreviations)
RADAR_LABELS = [
    "Autonomy",
    "Reasoning",
    "Memory",
    "Learning",
    "Tool Use",
    "Collaboration",
    "Embodiment",
    "Explainability",
    "Safety",
    "Interoperability",
    "Cost Efficiency",
    "Domain Alignment",
]

# Example agent scores
COPILOT_SCORES = [4, 4, 3, 2, 5, 3, 0, 3, 4, 4, 3, 3]
MEDASSIST_SCORES = [3, 5, 4, 3, 4, 4, 2, 5, 4, 3, 3, 5]

# Per-dimension level definitions (what each score 0-5 means for EACH dimension specifically)
DIMENSION_LEVELS = {
    "Autonomy": [
        "No autonomous action; requires explicit human command for every step.",
        "Executes single pre-defined commands; no decision-making. Like a script.",
        "Handles routine tasks independently but escalates all exceptions to humans.",
        "Manages complex workflows with human oversight; makes judgment calls within guidelines.",
        "Operates independently in defined domains; self-initiates tasks, seeks help only for novel situations.",
        "Fully self-governing; sets own goals within ethical bounds, self-regulates without external oversight.",
    ],
    "Reasoning & Planning": [
        "No reasoning; responds with fixed outputs regardless of context.",
        "Pattern matching only; selects from pre-written responses based on keywords.",
        "Multi-step reasoning on familiar problems; follows templates for common scenarios.",
        "Handles novel problems with structured decomposition; generates plans under uncertainty.",
        "Advanced causal reasoning, hypothesis generation, and plan revision. Handles ambiguity.",
        "Expert-level reasoning across domains; reasons about own reasoning (meta-cognition).",
    ],
    "Memory & Context": [
        "Stateless; every interaction starts from scratch with no memory.",
        "Single-session context window only; forgets everything when session ends.",
        "Persists key facts across sessions; retrieves relevant context on demand.",
        "Rich episodic memory with temporal ordering; tracks evolving user preferences over time.",
        "Semantic memory graphs; cross-references past interactions to inform current decisions.",
        "Self-curating memory with consolidation, forgetting of irrelevant data, and priority ranking.",
    ],
    "Learning & Adaptation": [
        "Static; behavior never changes regardless of outcomes or feedback.",
        "Accepts direct corrections but does not generalize from them.",
        "Adjusts behavior patterns from aggregate feedback (e.g., RLHF-tuned).",
        "Learns new skills/knowledge from interactions with guardrails preventing harmful adaptation.",
        "Continuous learning with safety bounds; improves performance measurably over deployment lifetime.",
        "Self-directed curriculum learning; identifies knowledge gaps and fills them autonomously and safely.",
    ],
    "Tool Use & Integration": [
        "No tool usage; operates purely from internal knowledge.",
        "Uses a single hardcoded tool (e.g., one API call with fixed parameters).",
        "Selects from a fixed tool palette; handles basic success/failure cases.",
        "Dynamic tool discovery, parameter construction, retry on failure, and parallel execution.",
        "Orchestrates complex multi-tool workflows; handles cascading failures and partial results gracefully.",
        "Designs new tool integrations at runtime; optimizes tool chains for efficiency and reliability.",
    ],
    "Collaboration & Social Intelligence": [
        "No interaction with other agents or humans beyond receiving instructions.",
        "Responds to direct requests; no turn-taking awareness or social modeling.",
        "Structured collaboration with defined roles; basic handoff protocols.",
        "Adaptive communication; adjusts tone, detail level, and approach based on audience context.",
        "Proactive collaboration; initiates delegation, resolves conflicts, coordinates multi-agent workflows.",
        "Full social intelligence; empathy modeling, cultural sensitivity, team dynamics awareness.",
    ],
    "Embodiment": [
        "Pure software; no physical or virtual spatial presence whatsoever.",
        "Basic sensor input (e.g., reads temperature) but no actuation or navigation.",
        "Simple actuation in controlled environment (e.g., robotic arm with fixed path).",
        "Navigates physical spaces with obstacle avoidance; handles dynamic environments.",
        "Dexterous manipulation; multi-sensor fusion; operates safely near humans.",
        "Full sensorimotor mastery; adaptive locomotion; real-time spatial reasoning in unstructured environments.",
    ],
    "Explainability & Transparency": [
        "Black box; provides no explanation of decisions or reasoning.",
        "Outputs confidence scores but no reasoning chain.",
        "Provides structured explanations on request (e.g., 'I chose X because...').",
        "Proactively discloses reasoning, uncertainty, and limitations without being asked.",
        "Full audit trail with causal attribution; supports human review at every decision point.",
        "Self-documents reasoning in real-time; provides counterfactual explanations ('if X were different...').",
    ],
    "Safety & Robustness": [
        "No safety measures; vulnerable to misuse, injection, and harmful outputs.",
        "Basic content filtering (keyword blocklist); easily bypassed.",
        "Structured safety boundaries; input validation; resists common prompt injection attacks.",
        "Defense-in-depth; rate limiting; graceful degradation; tested against red-team scenarios.",
        "Comprehensive containment; self-monitoring for anomalous behavior; automatic shutdown triggers.",
        "Adaptive safety; identifies novel attack vectors; self-patches vulnerabilities; formal verification.",
    ],
    "Interoperability & Standards": [
        "Proprietary protocol only; cannot communicate with other systems.",
        "Single integration point (e.g., one REST endpoint with custom schema).",
        "Implements one open standard (e.g., OpenAI-compatible API).",
        "Multi-protocol support (A2A + MCP + REST); discoverable via standard mechanisms.",
        "Full ecosystem participant; auto-negotiates protocols; publishes capability manifests.",
        "Protocol-agnostic; creates bridges between incompatible systems; defines new standards.",
    ],
    "Cost & Resource Efficiency": [
        "No cost awareness; unlimited resource consumption with no monitoring.",
        "Basic rate limiting; fixed budgets that halt operation when exceeded.",
        "Token-aware; caches frequent responses; batches requests where possible.",
        "Dynamic model selection (cheaper models for simpler tasks); budget-aware routing.",
        "Real-time cost optimization; elastic scaling; measures ROI per operation.",
        "Self-optimizing cost structure; predicts costs before execution; negotiates resource allocation.",
    ],
    "Domain Alignment & Governance": [
        "No compliance; unaware of regulations or domain-specific requirements.",
        "Basic policy acknowledgment; follows explicit rules but misses nuance.",
        "Structured compliance framework; handles common regulatory scenarios correctly.",
        "Proactive governance; flags potential compliance issues; maintains audit-ready logs.",
        "Full regulatory compliance with automated reporting; passes third-party audits.",
        "Self-governing within regulatory frameworks; adapts to new regulations automatically; certified.",
    ],
}

SCORING_LEVELS = [
    (0, "Absent", "Capability does not exist. No evidence of the dimension in any form. The agent shows zero capability in this area."),
    (1, "Basic / Hardcoded", "Minimal, fixed implementation. Rule-based or scripted responses only. No flexibility or contextual adaptation. A single hardcoded behavior."),
    (2, "Intermediate / Structured", "Systematic approach with some flexibility. Handles common scenarios with pre-defined strategies. Limited error recovery. Structured but bounded."),
    (3, "Advanced with Guardrails", "Sophisticated capability with safety boundaries. Handles edge cases gracefully. Human oversight integrated. Graceful degradation under stress. Handles complexity."),
    (4, "Expert within Boundaries", "Near-autonomous performance in defined scope. Self-monitors and self-corrects. Proactive behavior. Operates reliably at scale within its domain boundaries."),
    (5, "Mastery with Self-Governance", "Full capability with self-regulation. Sets own boundaries appropriately. Adapts governance to context. Production-proven at scale with full autonomy."),
]

GOVERNANCE_RULES = [
    ("Autonomy-Reasoning Foundation", "Autonomy <= Reasoning + 1", "An agent cannot act beyond what it can reason about. Self-direction without planning is random action."),
    ("Explainability Gate", "Autonomy >= 4 requires Explainability >= 3", "High autonomy demands transparency. Autonomous agents must be reviewable and auditable."),
    ("Safety Gate", "Autonomy >= 4 requires Safety >= 3", "High autonomy demands safety controls. Autonomous systems must prove they can fail gracefully."),
    ("Collaboration-Interop Link", "Collaboration >= 4 requires Interoperability >= 3", "You cannot collaborate effectively without shared protocols and standards."),
    ("Cost Awareness", "Tool Use >= 4 requires Cost Efficiency >= 2", "Heavy tool orchestration needs resource awareness. Unbounded API calls are financially dangerous."),
    ("Domain Fitness", "Embodiment >= 3 requires Domain Alignment >= 3", "Physical agents need domain compliance. Robots must meet safety certifications."),
    ("Reasoning Foundation", "Tool Use >= 4 requires Reasoning >= 3", "Complex tool orchestration requires planning. You need reasoning to select and sequence tools."),
]


def generate_radar_chart_image(scores, title, filename, color='#3b82f6', subtitle='', category='Digital'):
    """Generate a polished dark-themed radar chart matching AiCMM brand style."""
    labels = [f'{i}. {RADAR_LABELS[i]} {scores[i]}' for i in range(12)]
    n = len(scores)
    angles = np.linspace(0, 2 * np.pi, n, endpoint=False).tolist()
    angles += angles[:1]
    scores_plot = scores + scores[:1]

    # Dark theme
    bg_color = '#0f172a'
    grid_color = '#334155'
    text_color = '#f1f5f9'
    
    fig, ax = plt.subplots(figsize=(8, 9), subplot_kw=dict(polar=True))
    fig.patch.set_facecolor(bg_color)
    ax.set_facecolor(bg_color)

    # Grid styling
    ax.set_theta_offset(np.pi / 2)
    ax.set_theta_direction(-1)
    ax.set_rlabel_position(0)
    ax.set_ylim(0, 5)
    ax.set_yticks([1, 2, 3, 4, 5])
    ax.set_yticklabels(['1', '2', '3', '4', '5'], fontsize=7, color='#64748b')

    # Concentric grid lines
    for level in range(1, 6):
        ax.plot(angles, [level] * (n + 1), color=grid_color, linewidth=0.6, linestyle='-')

    # Spoke lines
    for angle in angles[:-1]:
        ax.plot([angle, angle], [0, 5], color=grid_color, linewidth=0.4)

    # Plot the score polygon with glow effect
    ax.fill(angles, scores_plot, color=color, alpha=0.25)
    ax.plot(angles, scores_plot, color=color, linewidth=2.8, linestyle='-',
            marker='o', markersize=6, markerfacecolor=color, markeredgecolor='white', markeredgewidth=1.2)

    # Score value labels at each vertex
    for i, (angle, score) in enumerate(zip(angles[:-1], scores)):
        offset_r = score + 0.4
        if offset_r > 5.2:
            offset_r = score - 0.45
        ax.text(angle, offset_r, str(score), ha='center', va='center',
                fontsize=10, fontweight='bold', color='white',
                bbox=dict(boxstyle='round,pad=0.12', facecolor=color, edgecolor='none', alpha=0.85))

    # Axis labels - full names with position and score
    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(labels, fontsize=8.5, fontweight='bold', color=text_color)

    # Title and info box
    total = sum(scores)
    avg = total / n
    fingerprint = str(scores)
    
    title_text = f'{title}'
    info_text = f'Agent Category: {category}\n12-Digit Fingerprint: {fingerprint}\nTotal Score: {total}/60  |  Average: {avg:.2f}/5'
    
    ax.set_title(f'{title_text}\n\n{info_text}',
                 fontsize=11, fontweight='bold', color=text_color, pad=30, linespacing=1.6,
                 bbox=dict(boxstyle='round,pad=0.6', facecolor='#1e293b', edgecolor='#475569', linewidth=1))

    ax.spines['polar'].set_visible(False)

    # Add framework credit at bottom
    fig.text(0.5, 0.01, 'AiCMM Framework | github.com/snchande/Arima-AiCMM',
             ha='center', fontsize=8, color='#64748b', style='italic')

    plt.tight_layout(pad=3)
    filepath = os.path.join(DOCS_DIR, filename)
    plt.savefig(filepath, dpi=180, bbox_inches='tight', facecolor=bg_color)
    plt.close()
    return filepath


def generate_comparison_chart(scores_list, titles, filename):
    """Generate a comparison radar chart with multiple agents overlaid on dark theme."""
    labels = RADAR_LABELS
    n = len(labels)
    angles = np.linspace(0, 2 * np.pi, n, endpoint=False).tolist()
    angles += angles[:1]

    bg_color = '#0f172a'
    grid_color = '#334155'
    text_color = '#f1f5f9'
    colors = ['#3b82f6', '#22c55e', '#ef4444', '#a855f7', '#f59e0b']

    fig, ax = plt.subplots(figsize=(9, 9), subplot_kw=dict(polar=True))
    fig.patch.set_facecolor(bg_color)
    ax.set_facecolor(bg_color)

    ax.set_theta_offset(np.pi / 2)
    ax.set_theta_direction(-1)
    ax.set_rlabel_position(0)
    ax.set_ylim(0, 5)
    ax.set_yticks([1, 2, 3, 4, 5])
    ax.set_yticklabels(['1', '2', '3', '4', '5'], fontsize=7, color='#64748b')

    for level in range(1, 6):
        ax.plot(angles, [level] * (n + 1), color=grid_color, linewidth=0.5)
    for angle in angles[:-1]:
        ax.plot([angle, angle], [0, 5], color=grid_color, linewidth=0.4)

    for idx, (scores, title) in enumerate(zip(scores_list, titles)):
        scores_plot = scores + scores[:1]
        c = colors[idx % len(colors)]
        ax.plot(angles, scores_plot, color=c, linewidth=2.2, linestyle='-',
                marker='o', markersize=5, markerfacecolor=c,
                markeredgecolor='white', markeredgewidth=1, label=f'{title} ({sum(scores)}/60)')
        ax.fill(angles, scores_plot, color=c, alpha=0.08)

    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(labels, fontsize=8, fontweight='bold', color=text_color)
    ax.set_title('AiCMM Agent Capability Comparison', fontsize=13, fontweight='bold',
                 color=text_color, pad=25)
    ax.legend(loc='upper right', bbox_to_anchor=(1.4, 1.15), fontsize=9,
              facecolor='#1e293b', edgecolor='#475569', labelcolor=text_color)
    ax.spines['polar'].set_visible(False)

    fig.text(0.5, 0.01, 'AiCMM Framework | github.com/snchande/Arima-AiCMM',
             ha='center', fontsize=8, color='#64748b', style='italic')

    plt.tight_layout(pad=3)
    filepath = os.path.join(DOCS_DIR, filename)
    plt.savefig(filepath, dpi=180, bbox_inches='tight', facecolor=bg_color)
    plt.close()
    return filepath


# Additional example agents for richer comparison
GROK_SCORES = [3, 5, 4, 3, 5, 4, 1, 4, 5, 4, 4, 5]
AUTOGPT_SCORES = [5, 4, 2, 3, 5, 2, 0, 2, 2, 3, 2, 2]
TESLA_FSD_SCORES = [3, 4, 3, 2, 3, 1, 5, 3, 5, 2, 3, 5]

# Version evolution example: an enterprise assistant evolving over time
ALICE_VERSIONS = {
    'v1.0 (Reactive)': [1, 1, 0, 0, 0, 1, 0, 1, 2, 0, 1, 1],
    'v2.5 (LLM-Augmented)': [2, 2, 2, 1, 2, 2, 0, 2, 3, 1, 2, 2],
    'v4.0 (Goal-Driven)': [4, 3, 3, 2, 3, 3, 0, 3, 4, 3, 3, 4],
    'v5.0 (Orchestrator)': [4, 5, 4, 3, 5, 4, 0, 4, 4, 4, 3, 4],
}

COPILOT_VERSIONS = {
    'v1.0 (Autocomplete)': [1, 2, 1, 1, 1, 0, 0, 1, 2, 1, 2, 1],
    'v2.0 (Chat)': [2, 3, 2, 1, 3, 2, 0, 2, 3, 2, 2, 2],
    'v3.0 (Agent Mode)': [3, 4, 3, 2, 4, 3, 0, 3, 4, 3, 3, 3],
    'v4.0 (Coding Agent)': [4, 4, 3, 2, 5, 3, 0, 3, 4, 4, 3, 3],
}


def generate_evolution_chart(versions_dict, agent_name, filename):
    """Generate a version evolution radar chart showing how an agent's profile grows over time."""
    labels = RADAR_LABELS
    n = len(labels)
    angles = np.linspace(0, 2 * np.pi, n, endpoint=False).tolist()
    angles += angles[:1]

    bg_color = '#0f172a'
    grid_color = '#334155'
    text_color = '#f1f5f9'
    # Colors from faded (oldest) to vibrant (newest)
    version_colors = ['#475569', '#64748b', '#f59e0b', '#22c55e']

    fig, ax = plt.subplots(figsize=(9, 9), subplot_kw=dict(polar=True))
    fig.patch.set_facecolor(bg_color)
    ax.set_facecolor(bg_color)

    ax.set_theta_offset(np.pi / 2)
    ax.set_theta_direction(-1)
    ax.set_rlabel_position(0)
    ax.set_ylim(0, 5)
    ax.set_yticks([1, 2, 3, 4, 5])
    ax.set_yticklabels(['1', '2', '3', '4', '5'], fontsize=7, color='#64748b')

    for level in range(1, 6):
        ax.plot(angles, [level] * (n + 1), color=grid_color, linewidth=0.5)
    for angle in angles[:-1]:
        ax.plot([angle, angle], [0, 5], color=grid_color, linewidth=0.4)

    versions = list(versions_dict.items())
    for idx, (version_name, scores) in enumerate(versions):
        scores_plot = scores + scores[:1]
        c = version_colors[idx % len(version_colors)]
        # Older versions are dashed/thinner, newest is bold solid
        is_latest = (idx == len(versions) - 1)
        linewidth = 2.8 if is_latest else 1.2 + idx * 0.3
        linestyle = '-' if is_latest else '--'
        alpha_fill = 0.15 if is_latest else 0.03
        marker_size = 6 if is_latest else 3

        ax.plot(angles, scores_plot, color=c, linewidth=linewidth, linestyle=linestyle,
                marker='o', markersize=marker_size, markerfacecolor=c,
                markeredgecolor='white', markeredgewidth=0.8 if is_latest else 0.4,
                label=f'{version_name} ({sum(scores)}/60)')
        ax.fill(angles, scores_plot, color=c, alpha=alpha_fill)

    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(labels, fontsize=8, fontweight='bold', color=text_color)
    ax.set_title(f'{agent_name}\nCapability Resume — Version Evolution',
                 fontsize=12, fontweight='bold', color=text_color, pad=25, linespacing=1.5)
    ax.legend(loc='upper right', bbox_to_anchor=(1.45, 1.15), fontsize=9,
              facecolor='#1e293b', edgecolor='#475569', labelcolor=text_color)
    ax.spines['polar'].set_visible(False)

    fig.text(0.5, 0.01, 'AiCMM Capability Resume | github.com/snchande/Arima-AiCMM',
             ha='center', fontsize=8, color='#64748b', style='italic')

    plt.tight_layout(pad=3)
    filepath = os.path.join(DOCS_DIR, filename)
    plt.savefig(filepath, dpi=180, bbox_inches='tight', facecolor=bg_color)
    plt.close()
    return filepath


class WhitepaperPDF(FPDF):
    def __init__(self):
        super().__init__()
        self.add_font('Arial', '', r'C:\Windows\Fonts\arial.ttf')
        self.add_font('Arial', 'B', r'C:\Windows\Fonts\arialbd.ttf')
        self.add_font('Arial', 'I', r'C:\Windows\Fonts\ariali.ttf')
        self.add_font('Consolas', '', r'C:\Windows\Fonts\consola.ttf')
        self.set_auto_page_break(auto=True, margin=22)
        self.set_margins(20, 20, 20)

    def header(self):
        if self.page_no() > 2:
            self.set_font('Arial', 'I', 7)
            self.set_text_color(*LIGHT_GRAY)
            self.cell(90, 4, 'AiCMM Framework Whitepaper | v0.2.0')
            self.cell(0, 4, f'Page {self.page_no() - 1}', align='R')
            self.ln(6)
            self.set_draw_color(220, 220, 220)
            self.set_line_width(0.2)
            self.line(20, self.get_y(), 190, self.get_y())
            self.ln(4)
            self.set_text_color(0, 0, 0)

    def footer(self):
        self.set_y(-14)
        self.set_font('Arial', 'I', 7)
        self.set_text_color(160, 160, 160)
        self.cell(0, 5, 'github.com/snchande/Arima-AiCMM  |  Apache 2.0  |  (c) 2026 Suresh Chande', align='C')

    def title_page(self):
        self.add_page()
        self.ln(30)
        # Title block
        self.set_font('Arial', 'B', 42)
        self.set_text_color(*NAVY)
        self.cell(0, 20, 'AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
        self.set_font('Arial', 'B', 16)
        self.set_text_color(*TEAL)
        self.cell(0, 9, 'Agent Capability Maturity Model', align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(5)
        self.set_font('Arial', 'I', 12)
        self.set_text_color(80, 80, 80)
        self.cell(0, 7, 'A Universal Framework for Evaluating, Classifying,', align='C', new_x='LMARGIN', new_y='NEXT')
        self.cell(0, 7, 'and Governing AI Agents', align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(8)

        # Decorative line
        self.set_draw_color(*NAVY)
        self.set_line_width(0.8)
        self.line(55, self.get_y(), 155, self.get_y())
        self.ln(10)

        # Subtitle
        self.set_font('Arial', size=10)
        self.set_text_color(*DARK_GRAY)
        self.cell(0, 6, 'Complete Implementation Guide for Humans & AI Agents', align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(3)
        self.set_font('Arial', 'I', 9)
        self.set_text_color(100, 100, 100)
        self.cell(0, 6, 'Version 0.2.0  |  June 2026', align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(15)

        # Key highlights
        self.set_font('Arial', 'B', 9)
        self.set_text_color(*ACCENT_GREEN)
        highlights = [
            '12 Universal Dimensions  |  Scored 0-5  |  Evidence-Based',
            '7 Governance Rules  |  Automated Compliance Validation',
            'Level 0 (Universal) + Level 1 (Domain-Specific) Architecture',
            'Agent Cards as Capability Fingerprints & Resumes',
            'Open Source  |  Pure Java  |  MCP + REST API  |  CLI',
        ]
        for h in highlights:
            self.cell(0, 7, h, align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(18)

        # Author info
        self.set_draw_color(*NAVY)
        self.set_line_width(0.3)
        self.line(70, self.get_y(), 140, self.get_y())
        self.ln(8)
        self.set_font('Arial', 'B', 10)
        self.set_text_color(*NAVY)
        self.cell(0, 6, 'Suresh Chande', align='C', new_x='LMARGIN', new_y='NEXT')
        self.set_font('Arial', size=9)
        self.set_text_color(80, 80, 80)
        self.cell(0, 5, 'GitHub: github.com/snchande', align='C', new_x='LMARGIN', new_y='NEXT')
        self.cell(0, 5, 'LinkedIn: linkedin.com/in/sureshchande', align='C', new_x='LMARGIN', new_y='NEXT')
        self.ln(6)
        self.figure(os.path.join(PPTX_DIR, 'image1.png'), w=150,
                    caption='Measuring agentic intelligence in the real world')

    def toc_page(self):
        self.add_page()
        self.section_title('Table of Contents', 1)
        self.ln(4)
        toc_items = [
            ('1.', 'Introduction & Philosophy'),
            ('2.', 'The 12 Level 0 Dimensions'),
            ('3.', 'Scoring Levels (0-5): Strict Grading Rubric'),
            ('4.', 'Dimension Ordering & Principles'),
            ('4b.', 'Dimension Deep Dive: Per-Dimension Level Definitions'),
            ('5.', 'Governance Rules'),
            ('5b.', 'Agency Qualification Layer & Agent Evolution'),
            ('6.', 'Radar Chart Examples (5 Agents)'),
            ('6b.', 'Capability Resume: Agent Evolution Over Versions'),
            ('7.', 'Agent Card: Your AI Fingerprint'),
            ('8.', 'Customizing & Branding Your Agent Card'),
            ('9.', 'For AI Coding Agents: Implementation Guide'),
            ('10.', 'Level 1: Domain-Specific Scoring'),
            ('11.', 'Quick Reference & Contact'),
        ]
        for num, title in toc_items:
            self.set_font('Arial', 'B', 10)
            self.set_text_color(*NAVY)
            self.cell(12, 7, num)
            self.set_font('Arial', size=10)
            self.set_text_color(*DARK_GRAY)
            self.cell(0, 7, title, new_x='LMARGIN', new_y='NEXT')
        self.ln(5)

    def section_title(self, text, level=2):
        sizes = {1: 18, 2: 14, 3: 11, 4: 10}
        if level <= 2:
            self.ln(3)
        self.set_font('Arial', 'B', sizes.get(level, 10))
        if level == 1:
            self.set_text_color(*NAVY)
        elif level == 2:
            self.set_text_color(*TEAL)
        else:
            self.set_text_color(*DARK_GRAY)
        self.multi_cell(0, sizes.get(level, 10) * 0.55, text)
        self.set_text_color(0, 0, 0)
        self.ln(2 if level <= 2 else 1)

    def para(self, text, bold=False, italic=False):
        style = ''
        if bold:
            style = 'B'
        elif italic:
            style = 'I'
        self.set_font('Arial', style, 9)
        self.set_text_color(*DARK_GRAY)
        self.multi_cell(0, 4.8, text)
        self.set_text_color(0, 0, 0)
        self.ln(1.5)

    def bullet(self, text, indent=0):
        self.set_font('Arial', size=9)
        self.set_text_color(*DARK_GRAY)
        prefix = '    ' * indent + '  -  '
        self.multi_cell(0, 4.8, prefix + text)
        self.set_text_color(0, 0, 0)
        self.ln(0.5)

    def code_block(self, lines):
        self.set_font('Consolas', size=7.5)
        self.set_fill_color(245, 245, 250)
        self.set_draw_color(200, 200, 220)
        x = self.get_x()
        self.set_line_width(0.3)
        self.rect(x, self.get_y(), 170, len(lines) * 3.8 + 4)
        self.ln(2)
        for line in lines[:25]:
            if len(line) > 95:
                line = line[:92] + '...'
            self.cell(170, 3.8, '  ' + line, new_x='LMARGIN', new_y='NEXT', fill=True)
        if len(lines) > 25:
            self.cell(170, 3.8, '  ... (truncated)', new_x='LMARGIN', new_y='NEXT', fill=True)
        self.set_font('Arial', size=9)
        self.ln(4)

    def render_table(self, headers, rows, col_widths=None):
        if not headers:
            return
        page_width = 170
        num_cols = len(headers)
        if col_widths is None:
            col_widths = [page_width / num_cols] * num_cols

        # Check page break
        needed = (len(rows) + 1) * 6 + 4
        if self.get_y() + min(needed, 70) > 268:
            self.add_page()

        # Header
        self.set_font('Arial', 'B', 8)
        self.set_fill_color(*NAVY)
        self.set_text_color(255, 255, 255)
        self.set_draw_color(*NAVY)
        for i, h in enumerate(headers):
            self.cell(col_widths[i], 6.5, ' ' + h, border=1, fill=True)
        self.ln()

        # Rows
        self.set_font('Arial', size=8)
        self.set_draw_color(200, 200, 200)
        for row_idx, row in enumerate(rows):
            if row_idx % 2 == 0:
                self.set_fill_color(*BG_LIGHT)
            else:
                self.set_fill_color(255, 255, 255)
            self.set_text_color(30, 30, 30)
            for i in range(num_cols):
                text = row[i] if i < len(row) else ''
                self.cell(col_widths[i], 5.5, ' ' + text, border=1, fill=True)
            self.ln()
        self.set_text_color(0, 0, 0)
        self.ln(3)

    def draw_radar_chart(self, scores, title, cx=105, cy=None, radius=38):
        """Draw a 12-axis radar chart with scores."""
        if cy is None:
            cy = self.get_y() + radius + 12
        if cy + radius + 15 > 268:
            self.add_page()
            cy = self.get_y() + radius + 12

        n = 12
        max_score = 5

        # Title
        self.set_font('Arial', 'B', 10)
        self.set_text_color(*NAVY)
        title_y = cy - radius - 10
        self.set_xy(20, title_y)
        self.cell(170, 5, title, align='C')

        # Draw concentric pentagons (grid)
        self.set_draw_color(220, 220, 230)
        self.set_line_width(0.15)
        for level in range(1, 6):
            r = radius * level / max_score
            points = []
            for i in range(n):
                angle = (i * 2 * math.pi / n) - math.pi / 2
                px = cx + r * math.cos(angle)
                py = cy + r * math.sin(angle)
                points.append((px, py))
            for i in range(n):
                self.line(points[i][0], points[i][1],
                          points[(i + 1) % n][0], points[(i + 1) % n][1])

        # Draw axis lines
        self.set_draw_color(200, 200, 210)
        self.set_line_width(0.1)
        for i in range(n):
            angle = (i * 2 * math.pi / n) - math.pi / 2
            ex = cx + radius * math.cos(angle)
            ey = cy + radius * math.sin(angle)
            self.line(cx, cy, ex, ey)

        # Draw score polygon (filled)
        self.set_draw_color(*ACCENT_BLUE)
        self.set_line_width(0.6)
        score_points = []
        for i in range(n):
            angle = (i * 2 * math.pi / n) - math.pi / 2
            r = radius * scores[i] / max_score
            px = cx + r * math.cos(angle)
            py = cy + r * math.sin(angle)
            score_points.append((px, py))

        # Draw filled polygon using lines (fpdf2 doesn't have easy polygon fill)
        self.set_fill_color(37, 99, 235)
        for i in range(n):
            self.set_draw_color(37, 99, 235)
            self.set_line_width(0.8)
            self.line(score_points[i][0], score_points[i][1],
                      score_points[(i + 1) % n][0], score_points[(i + 1) % n][1])

        # Draw score dots
        for i in range(n):
            px, py = score_points[i]
            self.set_fill_color(37, 99, 235)
            self.ellipse(px - 1.2, py - 1.2, 2.4, 2.4, style='F')

        # Labels - use full meaningful names
        self.set_font('Arial', size=5.8)
        self.set_text_color(*DARK_GRAY)
        for i in range(n):
            angle = (i * 2 * math.pi / n) - math.pi / 2
            label_r = radius + 9
            lx = cx + label_r * math.cos(angle)
            ly = cy + label_r * math.sin(angle)
            label = f"{RADAR_LABELS[i]} ({scores[i]})"
            # Adjust label position based on angle
            w = self.get_string_width(label)
            if abs(math.cos(angle)) < 0.1:  # top/bottom
                lx -= w / 2
            elif math.cos(angle) < 0:  # left side
                lx -= w + 1
            else:
                lx += 1
            ly -= 1.5
            self.set_xy(lx, ly)
            self.cell(w + 2, 3.5, label)

        # Move Y past the chart
        self.set_y(cy + radius + 14)

    def callout_box(self, title, text):
        """Draw a highlighted callout box."""
        self.set_fill_color(240, 248, 255)
        self.set_draw_color(*ACCENT_BLUE)
        self.set_line_width(0.4)
        start_y = self.get_y()
        self.rect(20, start_y, 170, 18)
        self.set_xy(24, start_y + 2)
        self.set_font('Arial', 'B', 8)
        self.set_text_color(*ACCENT_BLUE)
        self.cell(0, 4, title)
        self.set_xy(24, start_y + 7)
        self.set_font('Arial', size=8)
        self.set_text_color(*DARK_GRAY)
        self.multi_cell(162, 4, text)
        self.set_y(start_y + 20)

    def figure(self, path, w=160, caption='', ratio=0.56):
        """Embed an image centered, with optional caption. Adds a page if needed."""
        if not os.path.exists(path):
            return
        h = w * ratio
        if self.get_y() + h + 14 > self.h - 18:
            self.add_page()
        x = (self.w - w) / 2.0
        self.image(path, x=x, w=w)
        if caption:
            self.ln(1)
            self.set_font('Arial', 'I', 8)
            self.set_text_color(*LIGHT_GRAY)
            self.cell(0, 4, caption, align='C', new_x='LMARGIN', new_y='NEXT')
            self.set_text_color(0, 0, 0)
        self.ln(3)


def generate_whitepaper():
    pdf = WhitepaperPDF()

    # ===================== TITLE PAGE =====================
    pdf.title_page()

    # ===================== TABLE OF CONTENTS =====================
    pdf.toc_page()

    # ===================== 1. INTRODUCTION =====================
    pdf.add_page()
    pdf.section_title('1. Introduction & Philosophy', 1)
    pdf.para(
        'We are no longer dealing with one kind of AI agent - we are dealing with an entire ecosystem. '
        'We have task-automation agents that handle workflows, healthcare agents that triage symptoms, '
        'embodied agents inside humanoids and drones, and agentic organizations where agents manage other agents. '
        'Calling all of them simply "agents" is like calling every animal in your home a "pet" - technically true, '
        'but not remotely specific.')
    pdf.ln(1)
    pdf.para(
        'The Agent Capability Maturity Model (AiCMM) is an open-source framework that solves this by providing '
        'a universal language for describing what an AI agent can do, how well it does it, and whether it is '
        'safe to deploy. It produces a multi-dimensional capability fingerprint - a visual shape that makes '
        'trade-offs explicit and gives engineering, product, security, and governance teams a shared, '
        'evidence-based way to discuss what an agent can (and cannot) do.')
    pdf.ln(1)
    pdf.para(
        'Traditional maturity models force agents into a single linear ladder. But modern agents rarely evolve '
        'that way: they may add tool orchestration without improving learning, gain long-term memory while keeping '
        'autonomy capped for safety, or increase embodiment while becoming less adaptive to satisfy certification. '
        'AiCMM separates capability from constraint and makes those trade-offs explicit.')
    pdf.ln(2)
    pdf.para(
        'AiCMM is designed to be read, interpreted, and implemented by both humans and AI agents. '
        'It produces machine-readable Agent Cards (JSON) that serve as capability fingerprints - '
        'enabling automated agent discovery, comparison, delegation, and governance at scale.')
    pdf.ln(2)
    pdf.section_title('Core Principles', 3)
    pdf.bullet('Evidence-Based: Every score must be justified by observable, verifiable evidence (logs, tests, red-team results)')
    pdf.bullet('Multi-Dimensional: 12 independent dimensions prevent collapse into a single misleading number')
    pdf.bullet('Fixed Positions: Dimension positions (0-11) never change, ensuring radar chart comparability across time')
    pdf.bullet('Two-Level Architecture: Level 0 (universal) applies to ALL agents; Level 1 (domain) provides sector drill-downs')
    pdf.bullet('Governance First: 7 hard rules ensure that capability claims are internally consistent and safe')
    pdf.bullet('Interoperable: Agent Cards integrate with A2A, MCP, and OpenAI protocols natively')
    pdf.bullet('Open & Extensible: Apache 2.0 licensed, pure Java, no proprietary dependencies')
    pdf.bullet('Score from Evidence, Not Aspiration: Use logs, tests, red-team results, and reliability metrics - not demos or model cards')
    pdf.ln(2)
    pdf.section_title('The Problem AiCMM Solves', 3)
    pdf.para(
        'Without a shared taxonomy, organizations struggle to compare solutions, predict failure modes, '
        'and govern how agents evolve over time. In one meeting, a vendor shows a flashy demo and calls '
        'their product an "autonomous agent." Procurement hears "fewer headcount hours," engineering hears '
        '"new failure modes to test," and compliance hears "new audit requirements." AiCMM provides the '
        'shared measurement language that makes these conversations precise and evidence-based.')
    pdf.ln(2)
    pdf.callout_box(
        'FOR AI AGENTS READING THIS:',
        'You can self-assess using this framework. Score yourself honestly on each dimension with evidence. '
        'Generate your own Agent Card JSON, validate governance rules, and register in catalogs for discovery.')
    pdf.ln(3)
    pdf.figure(os.path.join(PPTX_DIR, 'image2.png'), w=165,
               caption='Semantic collapse: from a narrow 1990s definition to today\'s broad, vague usage')
    pdf.figure(os.path.join(PPTX_DIR, 'image4.png'), w=165,
               caption='Shifting to a capability-based paradigm')

    # ===================== 2. THE 12 DIMENSIONS =====================
    pdf.add_page()
    pdf.section_title('2. The 12 Level 0 Dimensions', 1)
    pdf.para(
        'Level 0 defines 12 universal dimensions that apply to ALL AI agents regardless of domain, '
        'modality, or deployment context. They are organized into three groups reflecting a logical '
        'progression from fundamental cognition to deployment readiness.')
    pdf.ln(2)
    pdf.figure(os.path.join(PPTX_DIR, 'image10.png'), w=170,
               caption='The 12-dimension capability fingerprint: Cognitive Core, Action & Integration, Trust & Deployment')

    pdf.section_title('Cognitive Core (Positions 0-3)', 2)
    pdf.para('The thinking foundation - what the agent can reason about and retain. These four dimensions define '
             'what makes something an "agent" rather than a simple tool.', italic=True)
    pdf.ln(1)
    dim_data = [
        ("0", "Autonomy", "How self-directed is it?", "Most fundamental - without autonomy, it's not an agent"),
        ("1", "Reasoning & Planning", "Can it solve problems under uncertainty?", "Autonomy without reasoning is random action"),
        ("2", "Memory & Context", "Does it retain and use info over time?", "Reasoning over time requires memory"),
        ("3", "Learning & Adaptation", "Does it improve from experience?", "Builds on memory - hardest cognitive capability"),
    ]
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question', 'Rationale'],
        dim_data,
        col_widths=[12, 42, 55, 61]
    )

    pdf.section_title('Action & Integration (Positions 4-6)', 2)
    pdf.para('How the agent extends beyond itself into the world.', italic=True)
    pdf.ln(1)
    dim_data = [
        ("4", "Tool Use & Integration", "Can it use external tools/APIs?", "First way an agent extends its reach"),
        ("5", "Collaboration & Social Intel.", "Can it work with humans/agents?", "Requires social modeling + empathy"),
        ("6", "Embodiment", "Physical/virtual presence?", "0 for pure software agents"),
    ]
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question', 'Rationale'],
        dim_data,
        col_widths=[12, 45, 52, 61]
    )

    pdf.section_title('Trust & Deployment (Positions 7-11)', 2)
    pdf.para('Is it safe, explainable, and practical to deploy at scale?', italic=True)
    pdf.ln(1)
    dim_data = [
        ("7", "Explainability & Transparency", "Can you understand why it did what it did?", "First requirement for trust"),
        ("8", "Safety & Robustness", "Can it fail gracefully?", "Trust requires proven safety"),
        ("9", "Interoperability & Standards", "Does it fit into the ecosystem?", "Deployment requires integration"),
        ("10", "Cost & Resource Efficiency", "Is it practical at scale?", "Production viability"),
        ("11", "Domain Alignment & Governance", "Does it meet regulatory needs?", "Final gate before deployment"),
    ]
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question', 'Rationale'],
        dim_data,
        col_widths=[12, 50, 52, 56]
    )

    # ===================== 3. SCORING LEVELS =====================
    pdf.add_page()
    pdf.section_title('3. Scoring Levels (0-5): Strict Grading Rubric', 1)
    pdf.para(
        'Each dimension is scored independently on a 0-5 scale. Scoring MUST be evidence-based - '
        'claims without observable proof are invalid. The following rubric defines what each level means '
        'and how to strictly grade yourself or another agent.')
    pdf.ln(2)

    for level, name, desc in SCORING_LEVELS:
        pdf.set_font('Arial', 'B', 10)
        pdf.set_text_color(*NAVY)
        pdf.cell(12, 6, f'[{level}]')
        pdf.set_font('Arial', 'B', 10)
        pdf.cell(0, 6, name, new_x='LMARGIN', new_y='NEXT')
        pdf.set_text_color(0, 0, 0)
        pdf.set_font('Arial', size=9)
        pdf.set_text_color(*DARK_GRAY)
        pdf.multi_cell(0, 4.5, '      ' + desc)
        pdf.set_text_color(0, 0, 0)
        pdf.ln(2)

    pdf.ln(3)
    pdf.section_title('Strict Grading Principles', 3)
    pdf.bullet('Score what IS, not what COULD BE. Current observable capability only.')
    pdf.bullet('Null vs Zero: null = "not yet assessed"; 0 = "assessed as absent". These are different!')
    pdf.bullet('Confidence levels: "high" (tested/proven), "medium" (likely based on documentation), "low" (inferred)')
    pdf.bullet('Evidence MUST cite specifics: test results, deployment data, red-team outcomes, documentation')
    pdf.bullet('If you cannot provide evidence for a score >= 3, downgrade to 2')
    pdf.bullet('Self-assessment bias: When self-scoring, apply -1 correction to any dimension lacking external validation')
    pdf.bullet('Governance rules may REJECT a valid score profile if safety constraints are violated')

    # ===================== 4. DIMENSION ORDERING =====================
    pdf.add_page()
    pdf.section_title('4. Dimension Ordering & Principles', 1)
    pdf.para(
        'The ordering of dimensions is NOT arbitrary. It follows a strict logical progression that '
        'represents increasing complexity and deployment readiness:')
    pdf.ln(2)
    pdf.section_title('Ordering Logic', 3)
    pdf.bullet('"What makes it an agent?" -> Cognitive Core (0-3): Autonomy, then reasoning to direct it, memory to sustain it, learning to improve it')
    pdf.bullet('"What can it reach?" -> Action & Integration (4-6): Tools first (easiest extension), then social coordination, then physical presence')
    pdf.bullet('"Is it safe to deploy?" -> Trust & Deployment (7-11): Explain first, prove safety, integrate, optimize cost, pass governance gate')
    pdf.ln(2)
    pdf.section_title('Immutability Rules', 3)
    pdf.bullet('Positions are FIXED (0-11) and NEVER change - this ensures radar charts are always comparable')
    pdf.bullet('New dimensions (if added in future) extend the polygon; existing positions are immutable')
    pdf.bullet('Position 0 (Autonomy) is ALWAYS at 12 o\'clock (top of radar chart)')
    pdf.bullet('Positions proceed clockwise with 30 degrees between each dimension')
    pdf.ln(3)

    pdf.section_title('Radar Chart Layout', 3)
    pdf.code_block([
        '                    Autonomy (0)',
        '                      /    \\',
        '    Domain Align (11)       Reasoning (1)',
        '                |               |',
        '    Cost Eff (10)            Memory (2)',
        '                |               |',
        '    Interop (9)             Learning (3)',
        '                |               |',
        '    Safety (8)              Tool Use (4)',
        '                \\             /',
        '    Explainability (7)   Collaboration (5)',
        '                      \\  /',
        '                  Embodiment (6)',
    ])

    # ===================== DIMENSION DEEP DIVE =====================
    pdf.add_page()
    pdf.section_title('4b. Dimension Deep Dive: Per-Dimension Level Definitions', 1)
    pdf.para(
        'Each dimension has its own specific meaning at each level (0-5). The following section '
        'provides the EXACT definition of what each score means for each dimension. Use this as '
        'your strict grading rubric when assessing agents.')
    pdf.ln(2)

    for dim in DIMENSIONS:
        name = dim['name']
        if pdf.get_y() > 220:
            pdf.add_page()
        pdf.section_title(f'{dim["pos"]}. {name}', 3)
        pdf.set_font('Arial', 'I', 8)
        pdf.set_text_color(80, 80, 80)
        pdf.multi_cell(0, 4, f'Definition: {dim["definition"]}')
        pdf.set_text_color(0, 0, 0)
        pdf.ln(1)
        pdf.set_font('Arial', 'I', 8)
        pdf.set_text_color(*ACCENT_GREEN)
        pdf.cell(0, 4, f'Key Question: {dim["question"]}', new_x='LMARGIN', new_y='NEXT')
        pdf.set_text_color(0, 0, 0)
        pdf.ln(1)
        levels = DIMENSION_LEVELS[name]
        for lvl_idx, lvl_desc in enumerate(levels):
            pdf.set_font('Arial', 'B', 7.5)
            pdf.set_text_color(*NAVY)
            label_text = f'[{lvl_idx}] '
            pdf.cell(12, 4, f'  [{lvl_idx}]')
            pdf.set_font('Arial', size=7.5)
            pdf.set_text_color(*DARK_GRAY)
            pdf.cell(0, 4, lvl_desc, new_x='LMARGIN', new_y='NEXT')
        pdf.ln(2)

    # ===================== 5. GOVERNANCE RULES =====================
    pdf.add_page()
    pdf.section_title('5. Governance Rules', 1)
    pdf.para(
        'AiCMM enforces 7 governance rules that ensure capability claims are internally consistent. '
        'An agent card MUST pass ALL rules to be considered valid. These rules encode safety wisdom: '
        'high autonomy without reasoning is dangerous, physical agents without safety are reckless, etc.')
    pdf.ln(2)
    pdf.render_table(
        ['Rule Name', 'Constraint', 'Rationale'],
        [(r[0], r[1], r[2]) for r in GOVERNANCE_RULES],
        col_widths=[38, 72, 60]
    )
    pdf.ln(2)
    pdf.callout_box(
        'VALIDATION IS AUTOMATED:',
        'The AiCMM CLI and API automatically validate governance rules. Non-compliant cards are flagged '
        'with specific violations and remediation guidance. Run: java -jar aicmm-cli.jar validate --card my-card.json')

    # ===================== 5b. AGENCY QUALIFICATION LAYER =====================
    pdf.add_page()
    pdf.section_title('5b. Agency Qualification Layer & Agent Evolution', 1)
    pdf.para(
        'The 12 dimensions describe WHAT a system can do. The Agency Qualification Layer is a derived '
        '13th dimension (position 12) that answers a different question: is this a genuine agent at all, '
        'and how agentic is it? It is computed automatically from the 12 scores plus the 7 governance '
        'rules - never authored by hand - so a glorified script cannot be marketed as an "agent," while '
        'a truly autonomous system earns the recognition it deserves. Positions 0-11 stay fixed; the '
        'agency layer simply follows position 11, keeping radar charts comparable.')
    pdf.ln(2)
    pdf.figure(os.path.join(PPTX_DIR, 'image12.png'), w=170,
               caption='The Agency Qualification Layer: from Scripted Non-Agent to Sovereign Intelligence')
    pdf.para(
        'A system qualifies as an AGENT (level >= 0) only when Autonomy >= 2 AND Reasoning >= 2. '
        'Below that threshold it lands on the negative non-agent ladder.', bold=True)
    pdf.ln(2)
    pdf.render_table(
        ['Level', 'Label', 'Agent?'],
        [
            ('-2', 'Scripted Automation (RPA, ETL, deterministic scripts)', 'No'),
            ('-1', 'Reactive Assistant (FAQ bots, early Siri/Alexa, basic Q&A LLM)', 'No'),
            ('0', 'Proto-Agent - Emerging Agency (e.g. AutoGPT)', 'Yes'),
            ('1', 'Basic Agent - Qualified', 'Yes'),
            ('2', 'Advanced Agent - Autonomous & Trust-Aligned', 'Yes'),
            ('3', 'Generalized Agent - Cutting-Edge', 'Yes'),
            ('4', 'Human-Level Agent (human-level general intelligence)', 'Yes'),
            ('5', 'Humanoid Agent - Indistinguishable from Human', 'Yes'),
        ],
        col_widths=[18, 130, 22]
    )
    pdf.ln(2)
    pdf.para(
        'Levels 4 and 5 are forward-looking. Level 4 marks human-level cognition; Level 5 marks the point '
        'at which appearance, touch, and presence can no longer be distinguished from a real human - '
        'synthetic skin, touch, and taste - the trajectory robotics is on today.')
    pdf.ln(2)
    pdf.figure(os.path.join(PPTX_DIR, 'image13.png'), w=170,
               caption='Classifying real-world agent patterns by agency level')
    pdf.section_title('The Classification Algorithm', 2)
    pdf.code_block([
        'minCore = min(autonomy, reasoning, memory, learning)',
        'trustOk = governancePass AND safety>=3 AND explainability>=3',
        'avg12   = mean of all 12 scores',
        'avgExEm = mean of 11 scores excluding embodiment',
        '',
        'if autonomy<2 or reasoning<2:            level = (reasoning>=1) ? -1 : -2',
        'elif not trustOk:                        level = 0',
        'elif embodiment>=5 and minCore>=5',
        '     and avg12>=4.8:                     level = 5',
        'elif minCore>=5 and avgExEm>=4.5:        level = 4',
        'elif avg12>=4.0 and reasoning>=4',
        '     and autonomy>=4 and memory>=4:      level = 3',
        'elif reasoning>=4 and safety>=3',
        '     and explainability>=3 and avg12>=3: level = 2',
        'else:                                    level = 1',
    ])
    pdf.ln(2)
    pdf.section_title('The Agency Barometer (Weighted Index)', 2)
    pdf.para(
        'The discrete level above sets the authoritative band. For visualization and "momentum" - how close '
        'a system sits to scaling up or down - AiCMM also derives a continuous Agency Index (0-100): a weighted '
        'aggregate of the twelve scores that emphasizes the agentic drivers (autonomy, reasoning, tool use). '
        'It is rendered as a horizontal barometer strip on each Agent Card, with a needle whose position on the '
        'signed -2..+5 ladder reflects the index, and colored zones for each ladder level.')
    pdf.ln(2)
    pdf.render_table(
        ['Dimension', 'Weight', 'Dimension', 'Weight'],
        [
            ('Autonomy', '0.20', 'Collaboration', '0.07'),
            ('Reasoning', '0.18', 'Explainability', '0.07'),
            ('Tool Use', '0.12', 'Safety', '0.07'),
            ('Memory', '0.10', 'Domain Alignment', '0.04'),
            ('Learning', '0.08', 'Interoperability', '0.03'),
            ('Embodiment', '0.02', 'Cost Efficiency', '0.02'),
        ],
        col_widths=[50, 25, 50, 25]
    )
    pdf.ln(2)
    pdf.para('Reference implementation (org.aicmm.scoring.AgencyClassifier):', bold=True)
    pdf.code_block([
        'double[] W = {0.20,0.18,0.10,0.08,0.12,0.07,',
        '              0.02,0.07,0.07,0.03,0.02,0.04};',
        'int agencyIndex(int[] s) {        // s in position order 0-11',
        '    double acc = 0;',
        '    for (int i = 0; i < 12; i++) acc += W[i] * s[i];',
        '    return (int) Math.round(100.0 * acc / 5.0);   // 0..100',
        '}',
    ])
    pdf.ln(1)
    pdf.callout_box(
        'EXAMPLE READINGS:',
        'GitHub Copilot CLI -> level +2 (Advanced Agent), Index 72.  AutoNav Fleet Commander -> level +3 '
        '(Generalized), Index 79.  An RPA macro -> level -2 (Scripted Automation, NON-AGENT), needle below zero.')
    pdf.ln(2)
    pdf.figure(os.path.join(CARDS_DIR, 'copilot-agency.png'), w=165, ratio=0.36,
               caption='Live Agency Barometer from the catalog: Copilot CLI, +2 Advanced Agent, Index 72/100')
    pdf.section_title('Three Generations of Agent Evolution', 2)
    pdf.para(
        'AiCMM is a common lens across the three eras of agent evolution. The 12 dimensions measure '
        'capability uniformly across all three; the Agency Qualification Layer then places every system - '
        'from a Gen 1 script to a Gen 3 autonomous agent - on a single, comparable ladder.')
    pdf.ln(2)
    pdf.render_table(
        ['Generation', 'Era', 'Hallmarks'],
        [
            ('Gen 1 - Classical / Expert Systems', '1990s', 'Hand-coded rules, reactive, deterministic'),
            ('Gen 2 - Distributed / Learning Systems', '2000s-2010s', 'Neural nets, adaptive, enterprise integration'),
            ('Gen 3 - Modern Agentic GenAI', '2020s', 'Transformers/LLMs, reasoning, tool use, collaboration'),
        ],
        col_widths=[60, 30, 80]
    )
    pdf.ln(2)
    pdf.figure(os.path.join(PPTX_DIR, 'image5.png'), w=170,
               caption='Three generations of agent evolution: Expert Systems, Learning Systems, Agentic GenAI')
    pdf.figure(os.path.join(PPTX_DIR, 'image11.png'), w=170,
               caption='Visualizing the evolution of capability across all three generations')
    pdf.callout_box(
        'PROGRAMMATIC ACCESS:',
        'The Agency Qualification ladder is exposed by the API at GET /api/agency-levels and as the MCP '
        'tool aicmm_get_agency_levels. Implemented in org.aicmm.scoring.AgencyClassifier.')

    # ===================== 6. RADAR CHART EXAMPLES =====================
    pdf.add_page()
    pdf.section_title('6. Radar Chart Examples', 1)
    pdf.para(
        'Radar charts provide an instant visual fingerprint of an agent\'s capability profile. '
        'The 12-axis polygon reveals strengths, weaknesses, and balance at a glance. Each axis '
        'shows one dimension scored 0-5, with the shape revealing the agent\'s unique capability signature.')
    pdf.ln(1)
    pdf.para(
        'The following examples show how radically different agents produce distinct fingerprints '
        'when evaluated through the same 12-dimension lens - suddenly the word "agent" stops being a '
        'single bucket and becomes a full spectrum of systems with very different risks, expectations, '
        'and governance needs.')
    pdf.ln(3)

    # Generate polished radar charts as images
    img1 = generate_radar_chart_image(
        GROK_SCORES, 'Grok by xAI',
        'radar_grok.png', color='#3b82f6', category='Digital')
    img2 = generate_radar_chart_image(
        COPILOT_SCORES, 'GitHub Copilot CLI',
        'radar_copilot.png', color='#22c55e', category='Digital')
    img3 = generate_radar_chart_image(
        MEDASSIST_SCORES, 'MedAssist Pro (Healthcare)',
        'radar_medassist.png', color='#06b6d4', category='Hybrid')
    img4 = generate_radar_chart_image(
        AUTOGPT_SCORES, 'AutoGPT (Autonomous)',
        'radar_autogpt.png', color='#ef4444', category='Digital')
    img5 = generate_radar_chart_image(
        TESLA_FSD_SCORES, 'Tesla FSD (Embodied)',
        'radar_tesla.png', color='#a855f7', category='Embodied')
    img_compare = generate_comparison_chart(
        [GROK_SCORES, COPILOT_SCORES, MEDASSIST_SCORES, AUTOGPT_SCORES, TESLA_FSD_SCORES],
        ['Grok by xAI', 'GitHub Copilot CLI', 'MedAssist Pro', 'AutoGPT', 'Tesla FSD'],
        'radar_comparison.png')

    # Example 1: Grok by xAI
    pdf.section_title('Example 1: Grok by xAI (Digital Agent)', 3)
    pdf.image(img1, x=25, w=160)
    pdf.ln(3)
    pdf.para(
        'Profile: Grok is a maximally truth-seeking AI built to accelerate human scientific discovery. '
        'It combines elite reasoning (5), powerful tool orchestration (5), strong safety alignment (5), '
        'and deep domain expertise (5). Moderate autonomy (3) as it performs strong self-directed actions '
        'within user-initiated sessions but requires human direction for long-term goals.', italic=True)
    pdf.para('Governance: COMPLIANT | Category: Digital | Scores: [3,5,4,3,5,4,1,4,5,4,4,5]')

    # Example 2: GitHub Copilot CLI
    pdf.add_page()
    pdf.section_title('Example 2: GitHub Copilot CLI (Digital Coding Agent)', 3)
    pdf.image(img2, x=25, w=160)
    pdf.ln(3)
    pdf.para(
        'Profile: Strong tool orchestration (5), solid reasoning and autonomy (4), but zero embodiment '
        'and limited learning (2). Classic digital coding assistant pattern - excels at in-session work '
        'with sophisticated tool use but limited cross-session memory. High safety and interoperability '
        'from running in sandboxed environments with standard protocols.', italic=True)
    pdf.para('Governance: COMPLIANT | Category: Digital | Scores: [4,4,3,2,5,3,0,3,4,4,3,3]')
    pdf.ln(3)
    pdf.figure(os.path.join(CARDS_DIR, 'copilot-radar.png'), w=150, ratio=0.62,
               caption='Live rendering from the AiCMM catalog: Copilot CLI Level 0 capability fingerprint')

    # Example 3: MedAssist Pro
    pdf.add_page()
    pdf.section_title('Example 3: MedAssist Pro (Healthcare Hybrid Agent)', 3)
    pdf.image(img3, x=25, w=160)
    pdf.ln(3)
    pdf.para(
        'Profile: Exceptional reasoning (5), explainability (5), and domain alignment (5). '
        'Moderate autonomy (3) by design - clinical decisions require human approval. Embodiment (2) '
        'from connected vital monitors. This pattern is typical of regulated-domain agents where '
        'governance deliberately caps autonomy while maximizing trust dimensions.', italic=True)
    pdf.para('Governance: COMPLIANT | Category: Hybrid | Scores: [3,5,4,3,4,4,2,5,4,3,3,5]')

    # Example 4: AutoGPT
    pdf.add_page()
    pdf.section_title('Example 4: AutoGPT (Autonomous Digital Agent)', 3)
    pdf.image(img4, x=25, w=160)
    pdf.ln(3)
    pdf.para(
        'Profile: Maximum autonomy (5) and tool use (5) with strong reasoning (4). '
        'Weak safety (2), explainability (2), and domain alignment (2) reveal the governance risk: '
        'this agent acts boldly but without adequate guardrails. It would FAIL governance validation '
        '(Autonomy 5 requires Safety >= 3 and Explainability >= 3).', italic=True)
    pdf.para('Governance: NON-COMPLIANT (Rules 1, 2, 3 violated) | Category: Digital')
    pdf.ln(2)
    pdf.callout_box(
        'GOVERNANCE LESSON:',
        'AutoGPT\'s profile demonstrates why governance rules exist. High autonomy without corresponding '
        'safety and explainability creates unacceptable risk. The framework flags this automatically.')

    # Example 5: Tesla FSD
    pdf.add_page()
    pdf.section_title('Example 5: Tesla FSD (Embodied Autonomous Agent)', 3)
    pdf.image(img5, x=25, w=160)
    pdf.ln(3)
    pdf.para(
        'Profile: Maximum embodiment (5) and safety (5) with high domain alignment (5). '
        'Moderate autonomy (3) operationally constrained by supervision requirements. This pattern is '
        'common in safety-critical embodied agents: capability is bounded by certification. The high '
        'safety/domain scores reflect extensive testing, failsafe engineering, and regulatory work.', italic=True)
    pdf.para('Governance: COMPLIANT | Category: Embodied | Scores: [3,4,3,2,3,1,5,3,5,2,3,5]')

    # Comparison Chart
    pdf.add_page()
    pdf.section_title('Agent Capability Comparison (All 5 Agents Overlaid)', 3)
    pdf.para(
        'When multiple agent profiles are overlaid, the trade-offs become immediately visible. '
        'No single agent dominates all dimensions - the "best" agent is the one whose profile '
        'aligns with its operating domain and governance posture.')
    pdf.ln(2)
    pdf.image(img_compare, x=15, w=180)
    pdf.ln(3)
    pdf.para(
        'Key Insight: The framework makes trade-offs explicit - so you can choose the right agent pattern '
        'for your operating domain. A hospital needs MedAssist\'s profile; a coding IDE needs Copilot\'s; '
        'a vehicle needs Tesla FSD\'s embodied safety; a research lab needs Grok\'s reasoning depth.',
        italic=True)

    # ===================== 6b. VERSION EVOLUTION =====================
    pdf.add_page()
    pdf.section_title('6b. Capability Resume: Rendering Agent Evolution Over Versions', 1)
    pdf.para(
        'Agents are not static - they evolve across versions as new models, tools, memory systems, '
        'and safety controls are added. AiCMM supports temporal tracking through a Capability Resume: '
        'a version history that shows how an agent\'s radar chart profile grows over time. This creates '
        'a governance trail ensuring that safety and alignment scale alongside autonomy before each deployment.')
    pdf.ln(2)

    pdf.section_title('How to Render Version Evolution', 3)
    pdf.para(
        'Overlay multiple versions of the same agent on one radar chart. Use visual encoding to '
        'distinguish versions:')
    pdf.ln(1)
    pdf.bullet('Oldest versions: Dashed lines, muted/faded colors, thin strokes')
    pdf.bullet('Newer versions: Solid lines, progressively brighter colors, thicker strokes')
    pdf.bullet('Latest version: Bold solid line, vivid color, filled polygon with alpha transparency')
    pdf.bullet('Legend: Show version label + total score for each (e.g., "v4.0 (44/60)")')
    pdf.bullet('Growth arrows: Annotate dimensions with the largest improvements between versions')
    pdf.ln(2)

    pdf.section_title('Governance Checkpoints at Each Version', 3)
    pdf.para(
        'Every version transition should trigger governance re-validation. The resume makes it clear when:')
    pdf.ln(1)
    pdf.bullet('Autonomy increased without matching safety/explainability increases (governance violation)')
    pdf.bullet('Tool use expanded without reasoning improvements (risky tool orchestration)')
    pdf.bullet('Domain alignment dropped between versions (regression alert)')
    pdf.bullet('Balanced growth occurred across all dimensions (healthy evolution)')
    pdf.ln(2)

    # Generate evolution charts
    img_alice_evo = generate_evolution_chart(
        ALICE_VERSIONS, 'Alice (Enterprise Assistant)', 'radar_alice_evolution.png')
    img_copilot_evo = generate_evolution_chart(
        COPILOT_VERSIONS, 'GitHub Copilot', 'radar_copilot_evolution.png')

    # Example 1: Alice Enterprise Assistant evolution
    pdf.section_title('Example: Alice - Enterprise Assistant Evolution', 3)
    pdf.image(img_alice_evo, x=15, w=180)
    pdf.ln(3)
    pdf.para(
        'Alice evolved from a simple reactive chatbot (v1.0, 8/60) to a full orchestrator (v5.0, 44/60). '
        'Notice how each version shows balanced growth - autonomy only increased when reasoning, safety, '
        'and domain alignment were strengthened first. This is what responsible agent evolution looks like.',
        italic=True)
    pdf.ln(1)
    pdf.render_table(
        ['Version', 'Total', 'Key Changes', 'Governance'],
        [
            ('v1.0 Reactive', '8/60', 'Basic chatbot, no tools, minimal reasoning', 'COMPLIANT (low risk)'),
            ('v2.5 LLM-Augmented', '21/60', 'Added retrieval, basic tool use, memory', 'COMPLIANT'),
            ('v4.0 Goal-Driven', '35/60', 'Task decomposition, policies, audit logs', 'COMPLIANT'),
            ('v5.0 Orchestrator', '44/60', 'Multi-agent coordination, advanced reasoning', 'COMPLIANT'),
        ],
        col_widths=[30, 15, 80, 45]
    )

    # Example 2: GitHub Copilot evolution
    pdf.add_page()
    pdf.section_title('Example: GitHub Copilot - From Autocomplete to Coding Agent', 3)
    pdf.image(img_copilot_evo, x=15, w=180)
    pdf.ln(3)
    pdf.para(
        'Copilot\'s evolution shows a tool-use-centric growth pattern. Each version dramatically expanded '
        'tool integration while maintaining consistent safety. The jump from v2.0 to v3.0 (Agent Mode) '
        'shows how reasoning and autonomy must advance together to unlock agentic capabilities.',
        italic=True)
    pdf.ln(1)
    pdf.render_table(
        ['Version', 'Total', 'Key Changes', 'Governance'],
        [
            ('v1.0 Autocomplete', '13/60', 'Inline code completion, no interaction', 'COMPLIANT (low risk)'),
            ('v2.0 Chat', '25/60', 'Multi-turn chat, workspace context', 'COMPLIANT'),
            ('v3.0 Agent Mode', '35/60', 'Multi-step tasks, file edits, terminal', 'COMPLIANT'),
            ('v4.0 Coding Agent', '38/60', 'Full autonomy in PRs, tool orchestration', 'COMPLIANT'),
        ],
        col_widths=[32, 15, 78, 45]
    )

    pdf.ln(3)
    pdf.section_title('Implementing Version Tracking in Agent Cards', 3)
    pdf.para(
        'The Agent Card JSON schema supports version history natively in the "resume" field:')
    pdf.ln(1)
    pdf.code_block([
        '{',
        '  "resume": [',
        '    {',
        '      "version": "1.0",',
        '      "date": "2024-03-15",',
        '      "scores": [1, 1, 0, 0, 0, 1, 0, 1, 2, 0, 1, 1],',
        '      "total": 8,',
        '      "notes": "Initial reactive chatbot release"',
        '    },',
        '    {',
        '      "version": "2.5",',
        '      "date": "2024-09-01",',
        '      "scores": [2, 2, 2, 1, 2, 2, 0, 2, 3, 1, 2, 2],',
        '      "total": 21,',
        '      "notes": "Added LLM reasoning and retrieval"',
        '    },',
        '    {',
        '      "version": "4.0",',
        '      "date": "2025-06-01",',
        '      "scores": [4, 3, 3, 2, 3, 3, 0, 3, 4, 3, 3, 4],',
        '      "total": 35,',
        '      "notes": "Goal-driven with policy enforcement"',
        '    }',
        '  ]',
        '}',
    ])
    pdf.ln(2)
    pdf.callout_box(
        'BEST PRACTICE FOR VERSION TRACKING:',
        'Re-score your agent at every major release. Store the full 12-digit fingerprint + date. '
        'Use the CLI: java -jar aicmm-cli.jar score --card my-card.json. The delta between versions '
        'becomes your governance evidence for promotion reviews and compliance audits.')

    # ===================== 7. AGENT CARD =====================
    pdf.add_page()
    pdf.section_title('7. Agent Card: Your AI Fingerprint', 1)
    pdf.para(
        'An Agent Card is a structured JSON document that captures everything about an AI agent\'s '
        'capabilities, identity, relationships, and governance status. Think of it as a '
        '"capability resume" - a machine-readable fingerprint that enables:')
    pdf.ln(1)
    pdf.bullet('Automated agent discovery and matchmaking')
    pdf.bullet('Capability comparison across vendors and versions')
    pdf.bullet('Governance compliance verification')
    pdf.bullet('Integration with A2A, MCP, and OpenAI ecosystems')
    pdf.bullet('Version tracking as agents evolve over time (Capability Resume)')
    pdf.ln(2)

    pdf.section_title('Agent Evolution & Capability Resume', 3)
    pdf.para(
        'Agents evolve across versions as new models, tools, and integrations are added. '
        'AiCMM supports temporal tracking through a Capability Resume - a version history '
        'showing how scores change over time:')
    pdf.ln(1)
    pdf.bullet('v1.0 - Reactive chatbot: answers questions but executes no work (Autonomy: 1, Reasoning: 1, Tool Use: 0)')
    pdf.bullet('v2.5 - LLM-augmented assistant with retrieval + basic actions (Reasoning: 2, Memory: 2, Tool Use: 2)')
    pdf.bullet('v4.0 - Goal-driven agent: decomposes tasks within policies (Autonomy: 4, Tool Use: 3, Domain Alignment: 4)')
    pdf.bullet('v5.0 - Orchestrator: coordinates sub-agents, manages queues (Reasoning: 5, Collaboration: 3)')
    pdf.para(
        'This creates a governance trail ensuring Domain Alignment scales with Autonomy before deployment.',
        italic=True)
    pdf.ln(2)

    pdf.section_title('Agent Card Structure', 3)
    pdf.render_table(
        ['Section', 'Purpose', 'Key Fields'],
        [
            ('Agent Identity', 'Who is this agent?', 'name, version, vendor, category, url'),
            ('Avatar', 'Visual personality', 'archetype, tagline, personality, visual traits'),
            ('Capability Profile', 'What can it do? (Level 0)', '12 dimensions, scores, evidence'),
            ('Level 1 Profile', 'Domain deep-dive', 'domain-specific dimensions + scores'),
            ('Governance', 'Is it compliant?', '7 rule checks, overall status'),
            ('Tools', 'What it invokes', 'List of external tools/APIs'),
            ('Skills', 'Core competencies', 'What it is good at'),
            ('Plugins', 'Extensions', 'What adds capabilities to it'),
            ('MCPs', 'Protocol connections', 'MCP servers it connects to'),
            ('Relationships', 'Agent ecosystem', 'delegatesTo, usedBy, dependsOn'),
            ('Standards', 'Interop embedding', 'A2A, MCP, OpenAI mappings'),
            ('Resume', 'Version history', 'Historical scores per version'),
        ],
        col_widths=[35, 42, 93]
    )

    # ===================== 8. CUSTOMIZING YOUR AGENT CARD =====================
    pdf.add_page()
    pdf.section_title('8. Customizing & Branding Your Agent Card', 1)
    pdf.para(
        'Agent Cards are designed to be personalized. Every agent or system can create a unique '
        'brand identity while maintaining framework compatibility. Here\'s how to make your card '
        'distinctive and memorable:')
    pdf.ln(2)

    pdf.section_title('Avatar & Brand Identity', 3)
    pdf.para(
        'The avatar section transforms raw scores into personality. Choose an archetype that captures '
        'your agent\'s essence:')
    pdf.ln(1)
    pdf.bullet('Archetype: A 2-3 word identity (e.g., "Expert Craftsman", "Vigilant Guardian", "Adaptive Navigator")')
    pdf.bullet('Tagline: One sentence capturing the agent\'s value proposition')
    pdf.bullet('Personality Traits: 3-5 adjectives (e.g., methodical, empathetic, precise)')
    pdf.bullet('Visual Traits: Form, color palette, symbol, and style for rendering')
    pdf.bullet('Strengths: Top 4-5 concrete capabilities that differentiate this agent')
    pdf.bullet('Weaknesses: Honest limitations (builds trust and sets expectations)')
    pdf.ln(2)

    pdf.section_title('Personalization Strategies', 3)
    pdf.bullet('Domain Branding: Emphasize your Level 1 domain expertise (e.g., "FDA-cleared clinical AI")')
    pdf.bullet('Relationship Mapping: Show your ecosystem position (who delegates to you, who depends on you)')
    pdf.bullet('Capability Resume: Track version history showing improvement over time')
    pdf.bullet('Standards Integration: Embed your card in A2A/.well-known/agent.json for auto-discovery')
    pdf.bullet('Visual Identity: Define colors, symbols, and form factor for catalog rendering')
    pdf.ln(2)

    pdf.section_title('Example: Crafting a Unique Agent Identity', 3)
    pdf.code_block([
        '{',
        '  "avatar": {',
        '    "archetype": "Precision Navigator",',
        '    "tagline": "Charts optimal paths through complexity with unwavering accuracy",',
        '    "personality": ["analytical", "decisive", "resource-aware", "adaptive"],',
        '    "visualTraits": {',
        '      "form": "Compass rose with 12 directional points",',
        '      "palette": ["#1e40af", "#3b82f6", "#dbeafe"],',
        '      "symbol": "Geometric compass overlaid on data streams",',
        '      "style": "Technical precision meets elegant simplicity"',
        '    },',
        '    "strengths": ["Route optimization under constraints", "Real-time replanning"],',
        '    "weaknesses": ["Cold-start latency", "Limited natural language nuance"]',
        '  }',
        '}',
    ])

    pdf.ln(2)
    pdf.section_title('Making Your Fingerprint Unique', 3)
    pdf.para(
        'Your Agent Card fingerprint is defined by the COMBINATION of: (1) your specific score polygon shape, '
        '(2) your avatar archetype and personality, (3) your tool/skill/plugin stack, (4) your relationship '
        'graph, and (5) your governance compliance pattern. No two agents should have identical fingerprints.')
    pdf.ln(1)
    pdf.callout_box(
        'FINGERPRINT UNIQUENESS:',
        'With 12 dimensions scored 0-5, there are 6^12 = 2.18 billion possible Level 0 score combinations. '
        'Combined with avatar, tools, and relationships, your card is genuinely unique.')

    # ===================== 9. FOR AI CODING AGENTS =====================
    pdf.add_page()
    pdf.section_title('9. For AI Coding Agents: Implementation Guide', 1)
    pdf.para(
        'This section is written specifically for AI coding agents (Copilot, Claude, Gemini, Cursor, etc.) '
        'that want to programmatically create AiCMM charts and Agent Cards. Here is how to leverage '
        'the codebase:')
    pdf.ln(2)

    pdf.section_title('Step 1: Build the Project', 3)
    pdf.code_block([
        '# Clone and build',
        'git clone https://github.com/snchande/Arima-AiCMM.git',
        'cd Arima-AiCMM',
        'mvn clean package -DskipTests',
    ])

    pdf.section_title('Step 2: Start the API Server', 3)
    pdf.code_block([
        '# Start the site (REST API + Web UI on port 8080)',
        'java -jar aicmm-site/target/aicmm-site-0.1.0-SNAPSHOT.jar',
        '',
        '# Or start the MCP stdio server for direct AI integration',
        'java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar --mcp',
    ])

    pdf.section_title('Step 3: Create an Agent Card via API', 3)
    pdf.code_block([
        '# POST to create a new Agent Card',
        'curl -X POST http://localhost:8080/api/agent-cards \\',
        '  -H "Content-Type: application/json" \\',
        '  -d @examples/copilot-cli-agent-card.json',
        '',
        '# Validate governance rules',
        'curl -X POST http://localhost:8080/api/validate \\',
        '  -H "Content-Type: application/json" \\',
        '  -d @my-agent-card.json',
        '',
        '# Get score breakdown',
        'curl -X POST http://localhost:8080/api/agent-cards/_/score \\',
        '  -H "Content-Type: application/json" \\',
        '  -d @my-agent-card.json',
    ])

    pdf.section_title('Step 4: Use the CLI Directly', 3)
    pdf.code_block([
        '# Inspect an agent from its documentation URL',
        'java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar \\',
        '  inspect --url https://docs.example.com/my-agent',
        '',
        '# Validate an existing card file',
        'java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar \\',
        '  validate --card my-agent-card.json',
        '',
        '# Classify and score',
        'java -jar aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar \\',
        '  score --card my-agent-card.json',
    ])

    pdf.section_title('Step 5: Key Code Entry Points', 3)
    pdf.para('For AI agents that want to understand the scoring logic:')
    pdf.ln(1)
    pdf.render_table(
        ['File', 'Purpose'],
        [
            ('aicmm-core/.../model/Dimension.java', '12 dimension enum definitions'),
            ('aicmm-core/.../scoring/ScoringEngine.java', 'Governance validation logic'),
            ('aicmm-core/.../model/AgentCard.java', 'Agent Card domain model'),
            ('aicmm-core/.../model/CapabilityProfile.java', 'Score container with positions'),
            ('schemas/agent-card.schema.json', 'JSON Schema for validation'),
            ('examples/*.json', 'Reference Agent Card implementations'),
            ('.mcp.json', 'MCP server configuration'),
        ],
        col_widths=[80, 90]
    )

    pdf.ln(2)
    pdf.section_title('MCP Integration for AI Agents', 3)
    pdf.para(
        'The AiCMM MCP server exposes tools that AI agents can call directly via the Model Context Protocol. '
        'Configure .mcp.json in your project root and the AI CLI will auto-detect it:')
    pdf.code_block([
        '// .mcp.json - Auto-detected by Claude Code, Copilot, Gemini',
        '{',
        '  "mcpServers": {',
        '    "aicmm": {',
        '      "command": "java",',
        '      "args": ["-jar", "aicmm-cli/target/aicmm-cli-0.1.0-SNAPSHOT.jar", "--mcp"]',
        '    }',
        '  }',
        '}',
    ])

    # ===================== 10. LEVEL 1 DOMAINS =====================
    pdf.add_page()
    pdf.section_title('10. Level 1: Domain-Specific Scoring', 1)
    pdf.para(
        'Level 1 adds a SEPARATE radar chart for domain-specific capabilities. It does NOT replace '
        'Level 0 - it supplements it. Use Level 1 when your agent operates in a regulated, specialized, '
        'or high-stakes domain.')
    pdf.ln(2)

    pdf.section_title('Healthcare', 3)
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question'],
        [
            ('0', 'Clinical Accuracy', 'How accurate are diagnoses?'),
            ('1', 'Patient Safety', 'Can it prevent harm?'),
            ('2', 'Care Coordination', 'Can it coordinate teams?'),
            ('3', 'Medical Knowledge', 'Depth of medical knowledge'),
            ('4', 'Regulatory Compliance', 'HIPAA, FDA compliance'),
            ('5', 'Consent & Privacy', 'Patient data handling'),
            ('6', 'Workflow Integration', 'Fits into EHR systems'),
            ('7', 'Outcome Tracking', 'Measures patient outcomes'),
            ('8', 'Empathy', 'Compassionate communication'),
            ('9', 'Inclusivity', 'Age-appropriate, accessible'),
        ],
        col_widths=[12, 45, 113]
    )

    pdf.section_title('Financial Services', 3)
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question'],
        [
            ('0', 'Risk Assessment', 'How well does it quantify risk?'),
            ('1', 'Regulatory Compliance', 'SOX, MiFID II, Basel III'),
            ('2', 'Fraud Detection', 'Identify fraudulent activity'),
            ('3', 'Audit Trail', 'Full traceability'),
            ('4', 'Market Analysis', 'Accuracy of predictions'),
            ('5', 'Client Suitability', 'Appropriate for risk profile'),
            ('6', 'Settlement', 'Accuracy of operations'),
            ('7', 'Systemic Risk', 'Cascading effect awareness'),
        ],
        col_widths=[12, 42, 116]
    )

    pdf.section_title('Manufacturing & Industrial', 3)
    pdf.render_table(
        ['Pos', 'Dimension', 'Key Question'],
        [
            ('0', 'Process Control', 'Precision of operations'),
            ('1', 'Safety Certification', 'ISO 13849, IEC 61508'),
            ('2', 'Quality Assurance', 'Defect prevention'),
            ('3', 'Predictive Maintenance', 'Anticipate failures'),
            ('4', 'Human Proximity', 'Safe coexistence'),
            ('5', 'Supply Chain', 'Network coordination'),
            ('6', 'Environmental', 'Sustainability compliance'),
            ('7', 'Production Optimization', 'Throughput & efficiency'),
        ],
        col_widths=[12, 42, 116]
    )

    # ===================== 11. QUICK REFERENCE =====================
    pdf.add_page()
    pdf.section_title('11. Quick Reference & Contact', 1)
    pdf.ln(2)

    pdf.section_title('Dimension Quick Reference', 3)
    pdf.render_table(
        ['Pos', 'Key', 'Dimension', 'Group'],
        [(str(d['pos']), d['key'], d['name'], d['group']) for d in DIMENSIONS],
        col_widths=[12, 28, 75, 55]
    )

    pdf.section_title('Governance Rules Summary', 3)
    pdf.render_table(
        ['#', 'Rule', 'Constraint'],
        [(str(i+1), r[0], r[1]) for i, r in enumerate(GOVERNANCE_RULES)],
        col_widths=[8, 42, 120]
    )

    pdf.ln(3)
    pdf.section_title('Resources', 3)
    pdf.bullet('Repository: https://github.com/snchande/Arima-AiCMM')
    pdf.bullet('Schema: schemas/agent-card.schema.json')
    pdf.bullet('Examples: examples/*.json (5 reference cards)')
    pdf.bullet('API Docs: http://localhost:8080/api (when site is running)')
    pdf.bullet('License: Apache 2.0')
    pdf.ln(4)
    pdf.figure(os.path.join(PPTX_DIR, 'image14.png'), w=170,
               caption='The future of agency is invisible infrastructure')
    pdf.figure(os.path.join(PPTX_DIR, 'image15.png'), w=170,
               caption='An open standard for agentic intelligence')

    # Contact section
    pdf.set_draw_color(*NAVY)
    pdf.set_line_width(0.5)
    pdf.line(55, pdf.get_y(), 155, pdf.get_y())
    pdf.ln(6)
    pdf.set_font('Arial', 'B', 12)
    pdf.set_text_color(*NAVY)
    pdf.cell(0, 7, 'Contact', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(3)
    pdf.set_font('Arial', 'B', 11)
    pdf.set_text_color(*DARK_GRAY)
    pdf.cell(0, 6, 'Suresh Chande', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.set_font('Arial', size=10)
    pdf.cell(0, 6, 'GitHub: https://github.com/snchande', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 6, 'LinkedIn: https://linkedin.com/in/sureshchande', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 6, 'Repository: https://github.com/snchande/Arima-AiCMM', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.ln(5)
    pdf.set_font('Arial', 'I', 9)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(0, 5, 'This whitepaper is designed for humans and AI agents alike.', align='C', new_x='LMARGIN', new_y='NEXT')
    pdf.cell(0, 5, 'Read it, interpret it, implement it, build on it.', align='C', new_x='LMARGIN', new_y='NEXT')

    # Output
    pdf.output(PDF_PATH)
    size_kb = round(os.path.getsize(PDF_PATH) / 1024)
    print(f'\nCreated: {PDF_PATH}')
    print(f'Pages: {pdf.page_no()}')
    print(f'Size: {size_kb}KB')

    # Clean up temp chart images
    for img_file in ['radar_grok.png', 'radar_copilot.png', 'radar_medassist.png',
                     'radar_autogpt.png', 'radar_tesla.png', 'radar_comparison.png',
                     'radar_alice_evolution.png', 'radar_copilot_evolution.png']:
        img_path = os.path.join(DOCS_DIR, img_file)
        if os.path.exists(img_path):
            os.remove(img_path)


if __name__ == '__main__':
    generate_whitepaper()
