const sharp = require('sharp');
const pngToIco = require('png-to-ico').default || require('png-to-ico');
const fs = require('fs');
const svg = fs.readFileSync('variant-F.svg');
const sizes = [16,32,48,64,128,256,512,1024];
fs.writeFileSync('aicmm-icon.svg', svg);
(async () => {
  for (const s of sizes) {
    await sharp(svg, { density: 384 }).resize(s, s).png().toFile(`aicmm-icon-${s}.png`);
  }
  const ico = await pngToIco(['aicmm-icon-16.png','aicmm-icon-32.png','aicmm-icon-48.png','aicmm-icon-64.png','aicmm-icon-128.png','aicmm-icon-256.png']);
  fs.writeFileSync('aicmm-icon.ico', ico);
  console.log('done');
})();
