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
    { position: 5,  key: 'collaboration',    label: 'Collaboration',    short: 'COL', color: '#0891b2' },
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

    const size = 200;
    const center = size / 2;
    const outerR = 80;
    const innerR = 40;

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;

    // Background polygon
    const angleStep = (2 * Math.PI) / numDims;
    let octPoints = [];
    for (let i = 0; i < numDims; i++) {
        const angle = getDimensionAngle(i, numDims);
        octPoints.push(`${center + outerR * Math.cos(angle)},${center + outerR * Math.sin(angle)}`);
    }
    svg += `<polygon points="${octPoints.join(' ')}" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2"/>`;

    // Capability petals
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        if (score === 0) return;

        const angle = getDimensionAngle(i, numDims);
        const nextAngle = getDimensionAngle((i + 1) % numDims, numDims);
        const petalR = innerR + (outerR - innerR) * (score / 5);

        const x1 = center + innerR * Math.cos(angle);
        const y1 = center + innerR * Math.sin(angle);
        const x2 = center + petalR * Math.cos(angle);
        const y2 = center + petalR * Math.sin(angle);
        const x3 = center + petalR * Math.cos((angle + nextAngle) / 2);
        const y3 = center + petalR * Math.sin((angle + nextAngle) / 2);
        const x4 = center + innerR * Math.cos(nextAngle);
        const y4 = center + innerR * Math.sin(nextAngle);

        const opacity = 0.3 + (score / 5) * 0.5;
        svg += `<path d="M ${x1} ${y1} L ${x2} ${y2} L ${x3} ${y3} L ${x4} ${y4} Z" fill="${dim.color}" opacity="${opacity}"/>`;
    });

    // Center circle
    svg += `<circle cx="${center}" cy="${center}" r="${innerR - 5}" fill="white" stroke="#e2e8f0" stroke-width="1"/>`;

    // Initials in center
    const initials = name.split(' ').map(w => w[0]).join('').substring(0, 2);
    svg += `<text x="${center}" y="${center + 2}" text-anchor="middle" dominant-baseline="middle" font-size="18" font-weight="bold" fill="#1e293b">${initials}</text>`;

    // Score labels around the edge
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        const angle = getDimensionAngle(i, numDims);
        const labelR = outerR + 12;
        const lx = center + labelR * Math.cos(angle);
        const ly = center + labelR * Math.sin(angle);
        svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="9" font-weight="600" fill="${dim.color}">${dim.short}${score}</text>`;
    });

    svg += '</svg>';
    container.innerHTML = svg;
}

/**
 * Renders a simple SVG radar chart for an agent capability profile.
 * Uses fixed dimension positions from LEVEL_0_DIMENSIONS.
 */
function renderRadarChart(container, mini = false) {
    const profile = JSON.parse(container.getAttribute('data-profile'));
    const dimensions = getActiveDimensions(profile);
    const numDims = dimensions.length;

    const size = mini ? 200 : 400;
    const center = size / 2;
    const maxRadius = center - (mini ? 30 : 60);
    const levels = 5;

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;
    svg += `<rect width="${size}" height="${size}" fill="#fafbfc" rx="8"/>`;

    // Draw grid circles with level labels
    for (let i = 1; i <= levels; i++) {
        const r = (maxRadius / levels) * i;
        svg += `<circle cx="${center}" cy="${center}" r="${r}" fill="none" stroke="#e2e8f0" stroke-width="1"/>`;
        if (!mini) svg += `<text x="${center + 4}" y="${center - r + 4}" font-size="9" fill="#94a3b8">${i}</text>`;
    }

    // Draw axes and labels using fixed positions
    dimensions.forEach((dim, i) => {
        const angle = getDimensionAngle(i, numDims);
        const x = center + maxRadius * Math.cos(angle);
        const y = center + maxRadius * Math.sin(angle);
        svg += `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="#e2e8f0" stroke-width="1"/>`;

        if (!mini) {
            const labelR = maxRadius + 35;
            const lx = center + labelR * Math.cos(angle);
            const ly = center + labelR * Math.sin(angle);
            svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="11" font-weight="500" fill="#475569">${dim.label}</text>`;
        }
    });

    // Draw the capability polygon
    let points = [];
    let nullPoints = []; // Track unscored dimensions for dashed display
    dimensions.forEach((dim, i) => {
        const val = profile[dim.key];
        const score = (val !== null && val !== undefined) ? (val.score ?? val) : null;
        const angle = getDimensionAngle(i, numDims);

        if (score === null) {
            // Unscored dimension — don't plot (leave at center)
            const x = center;
            const y = center;
            points.push(`${x},${y}`);
            nullPoints.push(i);
        } else {
            const r = (maxRadius / levels) * score;
            const x = center + r * Math.cos(angle);
            const y = center + r * Math.sin(angle);
            points.push(`${x},${y}`);
        }
    });

    svg += `<polygon points="${points.join(' ')}" fill="rgba(37, 99, 235, 0.15)" stroke="#2563eb" stroke-width="${mini ? 1.5 : 2.5}"/>`;

    // Draw score dots
    if (!mini) {
        dimensions.forEach((dim, i) => {
            const val = profile[dim.key];
            const score = (val !== null && val !== undefined) ? (val.score ?? val) : null;
            if (score === null) return; // Skip unscored
            const angle = getDimensionAngle(i, numDims);
            const r = (maxRadius / levels) * score;
            const x = center + r * Math.cos(angle);
            const y = center + r * Math.sin(angle);
            svg += `<circle cx="${x}" cy="${y}" r="5" fill="${dim.color}" stroke="white" stroke-width="2"/>`;
            svg += `<text x="${x}" y="${y - 12}" text-anchor="middle" font-size="12" font-weight="bold" fill="${dim.color}">${score}</text>`;
        });
    }

    // Total score in center (only count scored dimensions)
    const scored = dimensions.filter(dim => {
        const val = profile[dim.key];
        return val !== null && val !== undefined;
    });
    const total = scored.reduce((sum, dim) => {
        const val = profile[dim.key];
        return sum + ((val?.score ?? val) || 0);
    }, 0);
    const avg = scored.length > 0 ? (total / scored.length).toFixed(1) : '0.0';
    const maxTotal = scored.length * 5;
    const fontSize = mini ? 14 : 20;
    const subSize = mini ? 8 : 10;
    svg += `<text x="${center}" y="${center - 4}" text-anchor="middle" font-size="${fontSize}" font-weight="bold" fill="#1e293b">${total}</text>`;
    svg += `<text x="${center}" y="${center + (mini ? 8 : 12)}" text-anchor="middle" font-size="${subSize}" fill="#64748b">${mini ? avg : `/ ${maxTotal} (avg ${avg})`}</text>`;

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
