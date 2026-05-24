/**
 * AiCMM Site - Client-side JavaScript
 * Handles radar chart rendering for Agent Card capability profiles.
 */
document.addEventListener('DOMContentLoaded', () => {
    // Render radar charts for agent cards
    const radarCharts = document.querySelectorAll('.radar-chart[data-profile]');
    radarCharts.forEach(renderRadarChart);
});

/**
 * Renders a simple SVG radar chart for an agent capability profile.
 */
function renderRadarChart(container) {
    const profile = JSON.parse(container.getAttribute('data-profile'));
    const dimensions = [
        { key: 'autonomy', label: 'Autonomy' },
        { key: 'reasoning', label: 'Reasoning' },
        { key: 'learning', label: 'Learning' },
        { key: 'memory', label: 'Memory' },
        { key: 'toolUse', label: 'Tool Use' },
        { key: 'collaboration', label: 'Collaboration' },
        { key: 'embodiment', label: 'Embodiment' },
        { key: 'domainAlignment', label: 'Domain Align.' }
    ];

    const size = 350;
    const center = size / 2;
    const maxRadius = center - 50;
    const levels = 5;

    let svg = `<svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">`;

    // Draw grid circles
    for (let i = 1; i <= levels; i++) {
        const r = (maxRadius / levels) * i;
        svg += `<circle cx="${center}" cy="${center}" r="${r}" fill="none" stroke="#e2e8f0" stroke-width="1"/>`;
    }

    // Draw axes and labels
    const angleStep = (2 * Math.PI) / dimensions.length;
    dimensions.forEach((dim, i) => {
        const angle = angleStep * i - Math.PI / 2;
        const x = center + maxRadius * Math.cos(angle);
        const y = center + maxRadius * Math.sin(angle);
        svg += `<line x1="${center}" y1="${center}" x2="${x}" y2="${y}" stroke="#e2e8f0" stroke-width="1"/>`;

        // Label position (push further out)
        const labelR = maxRadius + 30;
        const lx = center + labelR * Math.cos(angle);
        const ly = center + labelR * Math.sin(angle);
        svg += `<text x="${lx}" y="${ly}" text-anchor="middle" dominant-baseline="middle" font-size="10" fill="#64748b">${dim.label}</text>`;
    });

    // Draw the capability polygon
    let points = [];
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        const angle = angleStep * i - Math.PI / 2;
        const r = (maxRadius / levels) * score;
        const x = center + r * Math.cos(angle);
        const y = center + r * Math.sin(angle);
        points.push(`${x},${y}`);
    });

    svg += `<polygon points="${points.join(' ')}" fill="rgba(37, 99, 235, 0.2)" stroke="#2563eb" stroke-width="2"/>`;

    // Draw score dots
    dimensions.forEach((dim, i) => {
        const score = profile[dim.key]?.score ?? 0;
        const angle = angleStep * i - Math.PI / 2;
        const r = (maxRadius / levels) * score;
        const x = center + r * Math.cos(angle);
        const y = center + r * Math.sin(angle);
        svg += `<circle cx="${x}" cy="${y}" r="4" fill="#2563eb"/>`;
        svg += `<text x="${x}" y="${y - 10}" text-anchor="middle" font-size="11" font-weight="bold" fill="#1d4ed8">${score}</text>`;
    });

    svg += '</svg>';
    container.innerHTML = svg;
}
