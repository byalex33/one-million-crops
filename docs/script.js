const search = document.querySelector('#placeholder-search');
const rows = [...document.querySelectorAll('.placeholder-row')];
const emptyState = document.querySelector('#empty-state');
const toast = document.querySelector('#copy-toast');
let toastTimer;

function filterPlaceholders() {
  const query = search.value.trim().toLowerCase();
  let visible = 0;

  rows.forEach((row) => {
    const matches = !query || `${row.dataset.search} ${row.textContent}`.toLowerCase().includes(query);
    row.hidden = !matches;
    if (matches) visible += 1;
  });

  emptyState.hidden = visible !== 0;
}

async function copyPlaceholder(row) {
  const value = row.querySelector('code').textContent;
  try {
    await navigator.clipboard.writeText(value);
    toast.textContent = `Copied ${value}`;
  } catch {
    toast.textContent = 'Copy failed — select the placeholder manually';
  }

  toast.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('visible'), 2200);
}

search.addEventListener('input', filterPlaceholders);
rows.forEach((row) => row.addEventListener('click', () => copyPlaceholder(row)));

document.addEventListener('keydown', (event) => {
  if (event.key === '/' && document.activeElement !== search) {
    event.preventDefault();
    search.focus();
  }
});
