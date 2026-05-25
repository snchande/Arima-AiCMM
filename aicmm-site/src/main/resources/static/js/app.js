/**
 * AiCMM Site - Client-side JavaScript
 * Handles radar chart rendering, avatar visualization, and card creation.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Render radar charts for agent cards
    const radarCharts = document.querySelectorAll('.radar-chart[data-profile]');
    radarCharts.forEach(renderRadarChart);

    // Render mini radar charts in catalog
    const miniRadars = document.querySelectorAll('.mini-radar-chart[data-profile]');
    miniRadars.forEach(el => renderRadarChart(el, true));

    // Render Level 1 domain radar charts
    const l1Charts = document.querySelectorAll('.level1-radar-chart[data-level1]');
    l1Charts.forEach(renderLevel1RadarChart);

    // Render avatar visuals
    const avatarVisuals = document.querySelectorAll('.avatar-visual[data-profile]');
    avatarVisuals.forEach(renderAvatarVisual);
});

/**
 * Level 0 Dimensions — Fixed positions (0..11), NEVER reorder.
 * Progression: Cognitive Core → Action & Integration → Trust & Deployment
 */
const LEVEL_0_DIMENSIONS = [
    { position: 0,  key: 'autonomy',         label: 'Autonomy',         short: 'AUT', color: '#2563eb' },
    { position: 1,  key: 'reasoning',        label: 'Reasoning',        short: 'REA', color: '#7c3aed' },
    { position: 2,  key: 'memory',           label: 'Memory',           short: 'MEM', color: '#d97706' },
    { position: 3,  key: 'learning',         label: 'Learning',         short: 'LRN', color: '#059669' },
    { position: 4,  key: 'toolUse',          label: 'Tool Use',         short: 'TUL', color: '#dc2626' },
    { position: 5,  key: 'collaboration',    label: 'Social Intelligence',    short: 'COL', color: '#0891b2' },
    { position: 6,  key: 'embodiment',       label: 'Embodiment',       short: 'EMB', color: '#4f46e5' },
    { position: 7,  key: 'explainability',   label: 'Explainability',   short: 'EXP', color: '#ea580c' },
    { position: 8,  key: 'safety',           label: 'Safety',           short: 'SAF', color: '#be123c' },
    { position: 9,  key: 'interoperability', label: 'Interoperability', short: 'INT', color: '#0d9488' },
    { position: 10, key: 'costEfficiency',   label: 'Cost Efficiency',  short: 'CST', color: '#ca8a04' },
    { position: 11, key: 'domainAlignment',  label: 'Domain Align.',    short: 'DOM', color: '#15803d' }
];

/**
 * Returns the active dimensions from a profile (scored or all Level 0).
 * Handles both legacy 8-dim profiles and new 12-dim profiles.
 */
function getActiveDimensions(profile) {
    // Check which dimensions have scores in the profile
    const scored = LEVEL_0_DIMENSIONS.filter(dim => {
        const val = profile[dim.key];
        return val !== undefined && val !== null;
    });
    // If profile has fewer than 9 dimensions scored, return only scored ones (backward compat)
    if (scored.length > 0 && scored.length < 9) {
        return scored;
    }
    // Otherwise return all Level 0 dimensions
    return LEVEL_0_DIMENSIONS;
}

/**
 * Fixed angle for a dimension position — NEVER changes regardless of total dimensions shown.
 * Position 0 is always at 12 o'clock (top), proceeding clockwise.
 */
function getDimensionAngle(position, totalDimensions) {
    return (position * (2 * Math.PI / totalDimensions)) - Math.PI / 2;
}

/**
 * Renders an SVG avatar based on the agent's capability profile.
 * Creates an octagonal badge with dimension-based visual encoding.
 */
function renderAvatarVisual(container) {
    const profile = JSON.parse(container.getAttribute('data-profile'));
    const name = container.getAttribute('data-name') || 'Agent';
    const archetype = container.getAttribute('data-archetype') || '';

    const dimensions = getActiveDimensions(profile);
    const numDims = dimensions.length;
    const uid = Math.random().toString(36).substr(2, 6);

    const size = 240;
    const center = size / 2;

    // Determine dominant group and color theme based on highest scores
    let groupScores = [0, 0, 0]; // cognitive, action, trust
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        if (i <= 3) groupScores[0] += score;
        else if (i <= 6) groupScores[1] += score;
        else groupScores[2] += score;
    });
    const dominantGroup = groupScores.indexOf(Math.max(...groupScores));
    const themes = [
        { primary: '#7c3aed', secondary: '#a78bfa', accent: '#ede9fe', glow: 'rgba(124,58,237,0.3)' },  // cognitive - purple
        { primary: '#2563eb', secondary: '#60a5fa', accent: '#dbeafe', glow: 'rgba(37,99,235,0.3)' },   // action - blue
        { primary: '#059669', secondary: '#34d399', accent: '#d1fae5', glow: 'rgba(5,150,105,0.3)' }    // trust - green
    ];
    const theme = themes[dominantGroup];

    // Calculate overall strength for visual weight
    const totalScore = dimensions.reduce((s, d) => s + (profile[d.key]?.score ?? 0), 0);
    const avgScore = totalScore / numDims;
    const strength = avgScore / 5; // 0-1

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;

    // Defs
    svg += `<defs>`;
    svg += `<radialGradient id="av-bg-${uid}"><stop offset="0%" stop-color="${theme.accent}"/><stop offset="100%" stop-color="white"/></radialGradient>`;
    svg += `<radialGradient id="av-core-${uid}"><stop offset="0%" stop-color="${theme.primary}" stop-opacity="0.9"/><stop offset="100%" stop-color="${theme.secondary}" stop-opacity="0.7"/></radialGradient>`;
    svg += `<filter id="av-glow-${uid}"><feGaussianBlur stdDeviation="4" result="blur"/><feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge></filter>`;
    svg += `<filter id="av-shadow-${uid}"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="${theme.primary}" flood-opacity="0.2"/></filter>`;
    svg += `</defs>`;

    // Background circle
    svg += `<circle cx="${center}" cy="${center}" r="${center - 8}" fill="url(#av-bg-${uid})" stroke="${theme.secondary}" stroke-width="2" stroke-opacity="0.3"/>`;

    // Orbital rings based on capability groups
    const ringRadii = [85, 70, 55];
    DIMENSION_GROUPS.forEach((group, gi) => {
        const groupDims = dimensions.filter((_, i) => group.positions.includes(i));
        const groupAvg = groupDims.reduce((s, d) => s + (profile[d.key]?.score ?? 0), 0) / (groupDims.length || 1);
        const opacity = 0.15 + (groupAvg / 5) * 0.4;
        const r = ringRadii[gi] * (0.7 + strength * 0.3);
        svg += `<circle cx="${center}" cy="${center}" r="${r}" fill="none" stroke="${group.color}" stroke-width="${1.5 + groupAvg * 0.3}" stroke-opacity="${opacity}" stroke-dasharray="${4 + groupAvg * 2} ${6 - groupAvg}"/>`;
    });

    // Capability nodes — positioned around concentric orbits
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        if (score === 0) return;

        const angle = getDimensionAngle(i, numDims);
        const orbitR = 40 + (score / 5) * 45;
        const x = center + orbitR * Math.cos(angle);
        const y = center + orbitR * Math.sin(angle);
        const nodeR = 3 + score * 1.2;

        svg += `<circle cx="${x}" cy="${y}" r="${nodeR}" fill="${dim.color}" opacity="${0.6 + score * 0.08}" filter="url(#av-glow-${uid})"/>`;

        // Connection lines from center to nodes
        svg += `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="${dim.color}" stroke-width="0.8" stroke-opacity="${0.15 + score * 0.06}" stroke-dasharray="2 3"/>`;
    });

    // Core emblem — hexagonal shape representing the agent's identity
    const coreR = 30 + strength * 8;
    const hexPoints = [];
    for (let i = 0; i < 6; i++) {
        const angle = (i * Math.PI / 3) - Math.PI / 6;
        hexPoints.push(`${center + coreR * Math.cos(angle)},${center + coreR * Math.sin(angle)}`);
    }
    svg += `<polygon points="${hexPoints.join(' ')}" fill="url(#av-core-${uid})" stroke="white" stroke-width="2.5" filter="url(#av-shadow-${uid})"/>`;

    // Inner decorative ring
    svg += `<circle cx="${center}" cy="${center}" r="${coreR - 8}" fill="none" stroke="white" stroke-width="1" stroke-opacity="0.5"/>`;

    // Initials
    const initials = name.split(/[\s-]+/).map(w => w[0]).join('').substring(0, 2).toUpperCase();
    svg += `<text x="${center}" y="${center - 2}" text-anchor="middle" dominant-baseline="middle" font-size="20" font-weight="800" fill="white" letter-spacing="1">${initials}</text>`;

    // Score badge at bottom
    svg += `<text x="${center}" y="${center + 14}" text-anchor="middle" font-size="9" fill="white" opacity="0.9">${avgScore.toFixed(1)}/5</text>`;

    // Category indicator — small icon at top
    const catIcon = archetype ? archetype.split(' ').pop().charAt(0).toUpperCase() : '●';
    svg += `<circle cx="${center}" cy="18" r="12" fill="white" stroke="${theme.primary}" stroke-width="1.5" filter="url(#av-shadow-${uid})"/>`;
    svg += `<text x="${center}" y="19" text-anchor="middle" dominant-baseline="middle" font-size="10" font-weight="700" fill="${theme.primary}">${catIcon}</text>`;

    // Outer decorative dots at cardinal positions for high-scoring dimensions
    const topDims = dimensions.slice().sort((a, b) => (profile[b.key]?.score ?? 0) - (profile[a.key]?.score ?? 0)).slice(0, 4);
    topDims.forEach((dim, i) => {
        const angle = (i * Math.PI / 2) - Math.PI / 4;
        const x = center + (center - 20) * Math.cos(angle);
        const y = center + (center - 20) * Math.sin(angle);
        svg += `<circle cx="${x}" cy="${y}" r="3" fill="${dim.color}" opacity="0.5"/>`;
    });

    svg += '</svg>';
    container.innerHTML = svg;
}

/**
 * Dimension Groups — defines the three visual sectors on the radar chart.
 * Each group gets a distinct background arc color and label.
 */
const DIMENSION_GROUPS = [
    { name: 'Cognitive Core', positions: [0, 1, 2, 3], color: '#7c3aed', bgColor: 'rgba(124, 58, 237, 0.04)', borderColor: 'rgba(124, 58, 237, 0.25)' },
    { name: 'Action & Integration', positions: [4, 5, 6], color: '#2563eb', bgColor: 'rgba(37, 99, 235, 0.04)', borderColor: 'rgba(37, 99, 235, 0.25)' },
    { name: 'Trust & Deployment', positions: [7, 8, 9, 10, 11], color: '#059669', bgColor: 'rgba(5, 150, 105, 0.04)', borderColor: 'rgba(5, 150, 105, 0.25)' }
];

/**
 * Renders a professional SVG radar chart with grouped dimension sectors.
 * Features: colored group arcs, gradient polygon fill, score dots, group labels.
 */
function renderRadarChart(container, mini = false) {
    const profile = JSON.parse(container.getAttribute('data-profile'));
    const dimensions = getActiveDimensions(profile);
    const numDims = dimensions.length;
    const hasGroups = numDims >= 12;

    const size = mini ? 220 : 480;
    const center = size / 2;
    const maxRadius = center - (mini ? 35 : 80);
    const levels = 5;
    const uid = Math.random().toString(36).substr(2, 6);

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;
    svg += `<rect width="${size}" height="${size}" fill="#ffffff" rx="12"/>`;

    // Gradient and filter definitions
    svg += `<defs>`;
    svg += `<radialGradient id="rg-${uid}"><stop offset="0%" stop-color="#2563eb" stop-opacity="0.22"/><stop offset="100%" stop-color="#7c3aed" stop-opacity="0.06"/></radialGradient>`;
    svg += `<filter id="glow-${uid}"><feGaussianBlur stdDeviation="2" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>`;
    svg += `<filter id="sh-${uid}"><feDropShadow dx="0" dy="1" stdDeviation="2" flood-opacity="0.1"/></filter>`;
    svg += `</defs>`;

    // Group sector backgrounds (12-dim full chart only)
    if (hasGroups && !mini) {
        DIMENSION_GROUPS.forEach(group => {
            const startPos = group.positions[0];
            const endPos = group.positions[group.positions.length - 1];
            const startAngle = getDimensionAngle(startPos, numDims) - (Math.PI / numDims);
            const endAngle = getDimensionAngle(endPos, numDims) + (Math.PI / numDims);
            const arcR = maxRadius + 18;

            const x1 = center + arcR * Math.cos(startAngle);
            const y1 = center + arcR * Math.sin(startAngle);
            const x2 = center + arcR * Math.cos(endAngle);
            const y2 = center + arcR * Math.sin(endAngle);
            const largeArc = (endAngle - startAngle) > Math.PI ? 1 : 0;

            svg += `<path d="M ${center} ${center} L ${x1} ${y1} A ${arcR} ${arcR} 0 ${largeArc} 1 ${x2} ${y2} Z" fill="${group.bgColor}" stroke="${group.borderColor}" stroke-width="1.5" stroke-dasharray="3 2"/>`;

            // Group label
            const midAngle = (startAngle + endAngle) / 2;
            const labelR = maxRadius + 58;
            const lx = center + labelR * Math.cos(midAngle);
            const ly = center + labelR * Math.sin(midAngle);
            svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="8.5" font-weight="700" fill="${group.color}" letter-spacing="0.8">${group.name.toUpperCase()}</text>`;
        });
    }

    // Grid circles
    for (let i = 1; i <= levels; i++) {
        const r = (maxRadius / levels) * i;
        svg += `<circle cx="${center}" cy="${center}" r="${r}" fill="none" stroke="#e2e8f0" stroke-width="${i === levels ? 1.5 : 0.75}"/>`;
        if (!mini) svg += `<text x="${center + 5}" y="${center - r + 4}" font-size="8" fill="#94a3b8" font-weight="500">${i}</text>`;
    }

    // Axes with colored endpoints
    dimensions.forEach((dim, i) => {
        const angle = getDimensionAngle(i, numDims);
        const x = center + maxRadius * Math.cos(angle);
        const y = center + maxRadius * Math.sin(angle);
        svg += `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="#e2e8f0" stroke-width="0.75"/>`;

        if (!mini) {
            const labelR = maxRadius + 30;
            const lx = center + labelR * Math.cos(angle);
            const ly = center + labelR * Math.sin(angle);
            svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="10" font-weight="600" fill="${dim.color}">${dim.label}</text>`;
        } else {
            const labelR = maxRadius + 14;
            const lx = center + labelR * Math.cos(angle);
            const ly = center + labelR * Math.sin(angle);
            svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="7" font-weight="600" fill="${dim.color}">${dim.short}</text>`;
        }
    });

    // Capability polygon
    let points = [];
    dimensions.forEach((dim, i) => {
        const val = profile[dim.key];
        const score = (val !== null && val !== undefined) ? (val.score ?? val) : null;
        const angle = getDimensionAngle(i, numDims);
        if (score === null || score === undefined) {
            points.push(`${center},${center}`);
        } else {
            const r = (maxRadius / levels) * score;
            points.push(`${center + r * Math.cos(angle)},${center + r * Math.sin(angle)}`);
        }
    });

    if (!mini) {
        svg += `<polygon points="${points.join(' ')}" fill="none" stroke="rgba(37,99,235,0.08)" stroke-width="8" filter="url(#glow-${uid})"/>`;
    }
    svg += `<polygon points="${points.join(' ')}" fill="url(#rg-${uid})" stroke="#2563eb" stroke-width="${mini ? 1.5 : 2.5}" stroke-linejoin="round"/>`;

    // Score dots with halos
    if (!mini) {
        dimensions.forEach((dim, i) => {
            const val = profile[dim.key];
            const score = (val !== null && val !== undefined) ? (val.score ?? val) : null;
            if (score === null || score === undefined) return;
            const angle = getDimensionAngle(i, numDims);
            const r = (maxRadius / levels) * score;
            const x = center + r * Math.cos(angle);
            const y = center + r * Math.sin(angle);
            svg += `<circle cx="${x}" cy="${y}" r="8" fill="${dim.color}" opacity="0.12"/>`;
            svg += `<circle cx="${x}" cy="${y}" r="4.5" fill="${dim.color}" stroke="white" stroke-width="2" filter="url(#sh-${uid})"/>`;
            svg += `<text x="${x}" y="${y - 13}" text-anchor="middle" font-size="11" font-weight="700" fill="${dim.color}">${score}</text>`;
        });
    }

    // Center badge
    const scored = dimensions.filter(dim => {
        const val = profile[dim.key];
        return val !== null && val !== undefined && (val.score ?? val) !== null;
    });
    const total = scored.reduce((sum, dim) => {
        const val = profile[dim.key];
        return sum + ((val?.score ?? val) || 0);
    }, 0);
    const avg = scored.length > 0 ? (total / scored.length).toFixed(1) : '0.0';
    const maxTotal = scored.length * 5;
    const centerR = mini ? 22 : 30;
    svg += `<circle cx="${center}" cy="${center}" r="${centerR}" fill="white" stroke="#e2e8f0" stroke-width="1.5" filter="url(#sh-${uid})"/>`;
    const fontSize = mini ? 13 : 18;
    const subSize = mini ? 7 : 9;
    svg += `<text x="${center}" y="${center - (mini ? 2 : 4)}" text-anchor="middle" font-size="${fontSize}" font-weight="800" fill="#1e293b">${total}</text>`;
    svg += `<text x="${center}" y="${center + (mini ? 8 : 11)}" text-anchor="middle" font-size="${subSize}" fill="#64748b" font-weight="500">${mini ? `/${maxTotal}` : `/ ${maxTotal} (avg ${avg})`}</text>`;

    svg += '</svg>';
    container.innerHTML = svg;
}

/**
 * Renders a Level 1 (domain-specific) radar chart.
 * Uses a distinct color scheme per domain with the same professional style.
 */
function renderLevel1RadarChart(container) {
    const data = JSON.parse(container.getAttribute('data-level1'));
    const domain = data.domain || 'unknown';
    const dims = data.dimensions || {};

    const domainColors = {
        healthcare: { primary: '#059669', gradient1: '#059669', gradient2: '#10b981', bg: 'rgba(5, 150, 105, 0.12)' },
        transportation: { primary: '#4f46e5', gradient1: '#4f46e5', gradient2: '#818cf8', bg: 'rgba(79, 70, 229, 0.12)' },
        finance: { primary: '#d97706', gradient1: '#d97706', gradient2: '#f59e0b', bg: 'rgba(217, 119, 6, 0.12)' },
        manufacturing: { primary: '#dc2626', gradient1: '#dc2626', gradient2: '#f87171', bg: 'rgba(220, 38, 38, 0.12)' }
    };
    const colors = domainColors[domain] || domainColors.healthcare;

    const dimList = Object.entries(dims).map(([key, val], i) => ({
        key, label: key.replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).trim(),
        score: val.score ?? 0, position: val.position ?? i
    })).sort((a, b) => a.position - b.position);

    const numDims = dimList.length;
    const size = 400;
    const center = size / 2;
    const maxRadius = center - 70;
    const levels = 5;
    const uid = Math.random().toString(36).substr(2, 6);

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;
    svg += `<rect width="${size}" height="${size}" fill="#ffffff" rx="12"/>`;
    svg += `<defs><radialGradient id="l1g-${uid}"><stop offset="0%" stop-color="${colors.gradient1}" stop-opacity="0.25"/><stop offset="100%" stop-color="${colors.gradient2}" stop-opacity="0.06"/></radialGradient>`;
    svg += `<filter id="l1s-${uid}"><feDropShadow dx="0" dy="1" stdDeviation="2" flood-opacity="0.1"/></filter></defs>`;

    // Domain label at top
    const domainLabel = domain.charAt(0).toUpperCase() + domain.slice(1);
    svg += `<text x="${center}" y="22" text-anchor="middle" font-size="11" font-weight="700" fill="${colors.primary}" letter-spacing="1">LEVEL 1 — ${domainLabel.toUpperCase()} DOMAIN</text>`;

    // Grid
    for (let i = 1; i <= levels; i++) {
        const r = (maxRadius / levels) * i;
        svg += `<circle cx="${center}" cy="${center + 10}" r="${r}" fill="none" stroke="#e2e8f0" stroke-width="${i === levels ? 1.5 : 0.75}"/>`;
        svg += `<text x="${center + 5}" y="${center + 10 - r + 4}" font-size="8" fill="#94a3b8">${i}</text>`;
    }

    // Axes and labels
    dimList.forEach((dim, i) => {
        const angle = getDimensionAngle(i, numDims);
        const cx = center, cy = center + 10;
        const x = cx + maxRadius * Math.cos(angle);
        const y = cy + maxRadius * Math.sin(angle);
        svg += `<line x1="${cx}" y1="${cy}" x2="${x}" y2="${y}" stroke="#e2e8f0" stroke-width="0.75"/>`;
        const labelR = maxRadius + 30;
        const lx = cx + labelR * Math.cos(angle);
        const ly = cy + labelR * Math.sin(angle);
        svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="9.5" font-weight="600" fill="${colors.primary}">${dim.label}</text>`;
    });

    // Polygon
    const cx = center, cy = center + 10;
    let points = dimList.map((dim, i) => {
        const angle = getDimensionAngle(i, numDims);
        const r = (maxRadius / levels) * dim.score;
        return `${cx + r * Math.cos(angle)},${cy + r * Math.sin(angle)}`;
    });
    svg += `<polygon points="${points.join(' ')}" fill="url(#l1g-${uid})" stroke="${colors.primary}" stroke-width="2.5" stroke-linejoin="round"/>`;

    // Dots
    dimList.forEach((dim, i) => {
        const angle = getDimensionAngle(i, numDims);
        const r = (maxRadius / levels) * dim.score;
        const x = cx + r * Math.cos(angle);
        const y = cy + r * Math.sin(angle);
        svg += `<circle cx="${x}" cy="${y}" r="4.5" fill="${colors.primary}" stroke="white" stroke-width="2" filter="url(#l1s-${uid})"/>`;
        svg += `<text x="${x}" y="${y - 12}" text-anchor="middle" font-size="11" font-weight="700" fill="${colors.primary}">${dim.score}</text>`;
    });

    // Center score
    const total = dimList.reduce((s, d) => s + d.score, 0);
    const avg = (total / numDims).toFixed(1);
    svg += `<circle cx="${cx}" cy="${cy}" r="28" fill="white" stroke="#e2e8f0" stroke-width="1.5" filter="url(#l1s-${uid})"/>`;
    svg += `<text x="${cx}" y="${cy - 3}" text-anchor="middle" font-size="16" font-weight="800" fill="${colors.primary}">${total}</text>`;
    svg += `<text x="${cx}" y="${cy + 11}" text-anchor="middle" font-size="8" fill="#64748b">avg ${avg}</text>`;

    svg += '</svg>';
    container.innerHTML = svg;
}

// === Create Card Form Functions ===

function updateScoreDisplay(input) {
    const dimKey = input.id.replace('score-', '');
    document.getElementById('display-' + dimKey).textContent = input.value;
}

function getFormScores() {
    const dims = ['autonomy', 'reasoning', 'memory', 'learning', 'toolUse', 'collaboration', 'embodiment', 'explainability', 'safety', 'interoperability', 'costEfficiency', 'domainAlignment'];
    const profile = {};
    dims.forEach(dim => {
        const el = document.getElementById('score-' + dim);
        profile[dim] = { score: el ? parseInt(el.value) : 0 };
    });
    return profile;
}

function previewRadar() {
    const profile = getFormScores();
    const preview = document.getElementById('preview-area');
    preview.style.display = 'block';

    const radarEl = document.getElementById('preview-radar');
    radarEl.setAttribute('data-profile', JSON.stringify(profile));
    renderRadarChart(radarEl);
}

function generateCard() {
    const form = document.getElementById('agent-card-form');
    const formData = new FormData(form);

    const profile = getFormScores();
    const total = Object.values(profile).reduce((s, d) => s + d.score, 0);

    const card = {
        schemaVersion: "0.1.0",
        agent: {
            name: formData.get('name') || 'Unnamed Agent',
            version: formData.get('version') || '1.0.0',
            vendor: formData.get('vendor') || '',
            description: formData.get('description') || '',
            category: formData.get('category') || 'digital',
            url: formData.get('url') || ''
        },
        capabilityProfile: profile,
        tools: splitComma(formData.get('tools')),
        skills: splitComma(formData.get('skills')),
        plugins: splitComma(formData.get('plugins')),
        mcpConnections: splitComma(formData.get('mcps')),
        agentRelationships: {
            delegatesTo: splitComma(formData.get('delegatesTo')),
            usedBy: splitComma(formData.get('usedBy')),
            dependsOn: splitComma(formData.get('dependsOn'))
        },
        sourceUrl: formData.get('agentUrl') || '',
        governanceCompliant: profile.autonomy.score <= (profile.domainAlignment.score + 1),
        totalScore: total,
        averageScore: parseFloat((total / 8).toFixed(2)),
        assessmentMetadata: {
            assessedBy: "Manual (via AiCMM Create Card)",
            assessedDate: new Date().toISOString().split('T')[0],
            methodology: "manual-expert",
            evidenceSources: formData.get('agentUrl') ? [formData.get('agentUrl')] : ["manual-input"]
        }
    };

    // Show preview
    const preview = document.getElementById('preview-area');
    preview.style.display = 'block';

    const radarEl = document.getElementById('preview-radar');
    radarEl.setAttribute('data-profile', JSON.stringify(profile));
    renderRadarChart(radarEl);

    const jsonEl = document.getElementById('preview-json');
    jsonEl.querySelector('code').textContent = JSON.stringify(card, null, 2);

    // Store for download
    window._generatedCard = card;
}

function downloadCard() {
    if (!window._generatedCard) return;
    const blob = new Blob([JSON.stringify(window._generatedCard, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const name = (window._generatedCard.agent.name || 'agent').toLowerCase().replace(/[^a-z0-9]+/g, '-');
    a.href = url;
    a.download = name + '-agent-card.json';
    a.click();
    URL.revokeObjectURL(url);
}

function copyCard() {
    if (!window._generatedCard) return;
    navigator.clipboard.writeText(JSON.stringify(window._generatedCard, null, 2));
    alert('Agent Card copied to clipboard!');
}

function splitComma(str) {
    if (!str) return [];
    return str.split(',').map(s => s.trim()).filter(s => s.length > 0);
}

/**
 * Catalog search and filter — filters the catalog table and card grid in real-time.
 */
function filterCatalog() {
    const query = (document.getElementById('catalog-search')?.value || '').toLowerCase();
    const categoryFilter = document.getElementById('filter-category')?.value || '';
    const minScore = parseInt(document.getElementById('filter-min-score')?.value || '0');

    const table = document.querySelector('.catalog-table');
    if (!table) return;

    const rows = table.querySelectorAll('tbody tr');
    let visibleCount = 0;

    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const cells = row.querySelectorAll('td');
        const category = cells[2]?.textContent.trim().toLowerCase() || '';
        const avgCell = cells[cells.length - 1];
        const avg = parseFloat(avgCell?.textContent || '0');

        const matchesQuery = !query || text.includes(query);
        const matchesCategory = !categoryFilter || category.includes(categoryFilter);
        const matchesScore = avg >= minScore;

        if (matchesQuery && matchesCategory && matchesScore) {
            row.style.display = '';
            visibleCount++;
        } else {
            row.style.display = 'none';
        }
    });

    // Also filter visual profile cards
    const cards = document.querySelectorAll('.catalog-card');
    const cardNames = Array.from(rows).filter(r => r.style.display !== 'none').map(r => r.querySelector('td a')?.textContent.toLowerCase());
    cards.forEach(card => {
        const name = card.querySelector('h3 a')?.textContent.toLowerCase() || '';
        card.style.display = cardNames.includes(name) ? '' : 'none';
    });

    const countEl = document.getElementById('search-results-count');
    if (countEl) {
        countEl.textContent = visibleCount + ' of ' + rows.length + ' agents shown';
    }
}
